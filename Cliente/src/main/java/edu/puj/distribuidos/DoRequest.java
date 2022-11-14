package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class DoRequest {
    /* Variables de pruebas */
    public final boolean testMode;
    private int nRequests = 0;
    private final int MAX_REQUESTS;

    private FileWriter log;

    static FileWriter createLogFile(UUID uuid) throws IOException {
        FileWriter writer = new FileWriter("CLIENT_" + uuid.toString() + ".txt");
        writer.write(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now()) + '\n');
        return writer;
    }

    DoRequest(String server, int maxreq) throws IOException {
        this.testMode = true;
        this.MAX_REQUESTS = maxreq;
        this.serverIp = server;
        this.log = createLogFile(this.clientUUID);
    }

    DoRequest() throws IOException {
        this.testMode = false;
        this.MAX_REQUESTS = 0;
        this.log = createLogFile(this.clientUUID);
    }

    public final UUID clientUUID = UUID.randomUUID(); // UUID identificador del cliente
    /* Variables para el control del fallback (Balanceador alternativo) */
    private String fallbackServer = null;
    private boolean systemHalted = false;
    private int consecutiveTries = 0;

    private final AtomicBoolean fallbackReady = new AtomicBoolean(true);

    /* Contexto de red y sockets */
    private ZContext context;
    private ZMQ.Socket socket;

    private String serverIp;

    private final Scanner scanner = new Scanner(System.in); // Scanner para los menús
    private final Random random = new Random(); // Random para el modo de test
    private Long totalTime = 0L;

    public String makeRequest(String request, Thread workerCheck) {
        System.out.println("Enviando solicitud...");
        long startTime = System.currentTimeMillis();

        // Enviar una petición
        socket.sendMore(clientUUID.toString().getBytes(ZMQ.CHARSET));
        socket.send(request.getBytes(ZMQ.CHARSET));
        workerCheck.start();

        // Recibir la respuesta
        byte[] response = socket.recv();

        // Obtener tiempo
        long endTime = System.currentTimeMillis();
        consecutiveTries = 0;
        workerCheck.interrupt();

        // Mostrar la respuesta
        System.out.println("Solicitud completada en: " + (endTime - startTime) + "ms");
        try {
            log.write("Solicitud (" + request + ") -> " + (endTime - startTime) + "ms\n");
            log.flush();
        } catch (Exception ignored) {
        }
        totalTime += (endTime - startTime);
        System.out.println("\nRespuesta del servidor: ");
        return new String(response, ZMQ.CHARSET);
    }

    /**
     * Establecer el servidor de fallback
     *
     * @param serverConn IP del servidor de fallback
     */
    public Boolean setFallbackServer(String serverConn) {
        if ((fallbackServer != null) && (serverConn.equals("NOALT"))) {
            fallbackServer = null;
            return true;
        } else if ((fallbackServer == null) && (!serverConn.equals("NOALT"))) {
            fallbackServer = serverConn;
            return true;
        }
        return false;
    }

    /**
     * Comenzar una nueva conexión con el servidor
     *
     * @param serverIp Dirección ip del servidor (Balanceador)
     */
    public void startConnection(String serverIp) {
        // Crear el contexto
        context = new ZContext();

        // Crear el Socket y darle una identidad (UUID)
        System.out.println("\nConectando al servidor... " + serverIp);
        socket = context.createSocket(SocketType.REQ);
        socket.setIdentity(clientUUID.toString().getBytes(ZMQ.CHARSET));

        // Conectarse al servidor
        socket.connect("tcp://" + serverIp + ":" + Main.PORT);
        System.out.println("Conectado a " + serverIp);
    }

    /**
     * Detener todas las conexiones
     */
    public void stopConnection() {
        socket.close();
        context.close();
        context.destroy();
        System.err.println("Conexión detenida");
    }

    public void main() {
        // Iniciar el cliente
        System.out.println("Cliente - Gestión de productos");
        System.out.println("UUID del cliente: " + clientUUID);

        // Obtener datos del servidor
        System.out.print("Digite la dirección IP del Servidor: ");
        if (!testMode) serverIp = scanner.nextLine();

        // Inicializar la conexión
        startConnection(serverIp);

        // Conectarse al servicio de HealthCheck
        Thread healthCheck = new Thread(new BalancerHealthCheck(serverIp, context, this::setFallbackServer));

        // Manejador de errores de HealthCheck (Cuando el servidor falla)
        Thread.UncaughtExceptionHandler failedHealthCheck = (t, e) -> {
            if (e instanceof ServerNotResponding) {
                healthCheck.interrupt();
                try {
                    healthCheck.join();
                } catch (InterruptedException ignored) {
                }

                // Si no hay fallback configurado
                if (fallbackServer == null) {
                    System.err.println("El servidor ha dejado de funcionar, intenta más tarde...");
                    systemHalted = true;
                    System.exit(1);
                }

                // Cambiar a servidor principal
                fallbackReady.set(false); // Lock (Mientras se logra realizar la conexión)
                System.err.println("Servidor no responde, cambiando al servidor de Fallback");
                stopConnection();
                startConnection(fallbackServer);
                fallbackServer = null;
                fallbackReady.set(true); // Liberar la conexión

                try {
                    log.write("(!) Cambiado al servidor alternativo: " + serverIp);
                } catch (Exception ignored) {
                }

            } else if (e instanceof WorkerNotResponding) {
                if (consecutiveTries >= Main.WORKER_CHECK_MAX_TRIES) {
                    System.err.println("No hay servidores disponibles, intenta más tarde");
                    System.exit(1);
                }

                System.err.println("Reiniciando petición (" + consecutiveTries + ")");
                fallbackReady.set(false);
                stopConnection();
                startConnection(serverIp);
                consecutiveTries++;
                fallbackReady.set(true);

                try {
                    log.write("(!) Reiniciada petición, Worker no responde\n");
                } catch (Exception ignored) {
                }
            }
        };

        // Iniciar el servicio de HealthCheck
        healthCheck.setUncaughtExceptionHandler(failedHealthCheck);
        healthCheck.start();

        // Handle para cuando el programa se acabe
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Desconectarse
            System.out.println("Desconectándose del sistema...");
            systemHalted = true;
            stopConnection();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        // Mostrar el menú
        int opcion = 0;
        boolean wasInterrupted = false; // Flag para saber si la operación fue interrumpida y no mostrar
        do {
            if (systemHalted) break;

            // Create WorkerThread
            Thread workerCheck = new Thread(new WorkerCheck(clientUUID, serverIp, context));
            workerCheck.setUncaughtExceptionHandler(failedHealthCheck);

            if (!wasInterrupted && !testMode) {
                System.out.println("\nSistema de gestión de productos:");
                System.out.println("1. Listar productos");
                System.out.println("2. Descripción del producto");
                System.out.println("3. Comprar un producto");
                System.out.println("4. PING a worker");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();
            }

            if (testMode) opcion = random.nextInt(3) + 1;

            // Parsear la desición
            try {
                // Tomar un camino dependiendo de la opción
                if (fallbackReady.get()) {
                    // Seleccionar la opción
                    switch (opcion) {
                        default -> System.out.println("Opción desconocida");
                        case 0 -> {
                            System.out.println("Saliendo...");
                            workerCheck.interrupt();
                        }
                        case 1 -> {
                            System.out.println("Consultando lista de productos: ");
                            System.out.println(makeRequest("LIST", workerCheck));
                        }
                        case 2 -> {
                            System.out.print("Digite el ID del producto a consultar: ");
                            int idProducto;
                            if (testMode) idProducto = random.nextInt(10);
                            else idProducto = scanner.nextInt();
                            System.out.println("Consultando producto " + idProducto);
                            System.out.println(makeRequest(("QUERY " + idProducto), workerCheck));
                        }
                        case 3 -> {
                            System.out.print("Digite el ID del producto a comprar: ");
                            int idProducto;
                            if (testMode) idProducto = random.nextInt(10);
                            else idProducto = scanner.nextInt();
                            System.out.println("Comprando producto " + idProducto);
                            System.out.println(makeRequest(("BUY " + idProducto), workerCheck));
                        }
                        case 4 -> System.out.println(makeRequest("PING", workerCheck));
                    }

                    // La operación pudo completarse correctamente
                    wasInterrupted = false;
                }
            } catch (ZMQException ex) {
                // En caso de que haya error de conexión se activa el flag de interrupción
                wasInterrupted = true;
            }

            // Unir el Hilo
            try {
                workerCheck.join();
                nRequests++;
            } catch (InterruptedException ignored) {
            }

            // TEST MODE
            if ((nRequests >= MAX_REQUESTS) && testMode)
                System.exit(0);

        } while (opcion != 0);

        // Exit
        try {
            log.write("Terminado...");
            log.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
