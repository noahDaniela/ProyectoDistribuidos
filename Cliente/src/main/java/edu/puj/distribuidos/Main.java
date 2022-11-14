package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

    /* Puertos */
    public static final Integer PORT = 5550; // Puerto del Servidor
    public static final Integer HEALTH_CHECK_PORT = 5570; // Puerto de HEALTH_CHECK
    public static final Integer WORKER_CHECK_PORT_CLIENT = 5572; // Puerto de WORKER_CHECK
    public static final Integer WORKER_CHECK_MAX_TRIES = 3; // Puerto de WORKER_CHECK

    /* Otras variables */
    public static final UUID clientUUID = UUID.randomUUID(); // UUID identificador del cliente
    public static final Integer HEALTH_CHECK_TIMEOUT = 5000;

    /* Variables para el control del fallback (Balanceador alternativo) */
    private static String fallbackServer = null;
    private static boolean systemHalted = false;
    private static int consecutiveTries = 0;

    private static final AtomicBoolean fallbackReady = new AtomicBoolean(true);

    /* Contexto de red y sockets */
    private static ZContext context;
    private static ZMQ.Socket socket;

    public static String makeRequest(String request, Thread workerCheck) {
        System.out.println("Enviando solicitud...");
        // Enviar una petición
        socket.sendMore(clientUUID.toString().getBytes(ZMQ.CHARSET));
        socket.send(request.getBytes(ZMQ.CHARSET));
        workerCheck.start();
        // Recibir la respuesta
        byte[] response = socket.recv();
        workerCheck.interrupt();
        // Mostrar la respuesta
        consecutiveTries = 0;
        System.out.println("Respuesta del servidor: ");
        return new String(response, ZMQ.CHARSET);
    }

    /**
     * Establecer el servidor de fallback
     *
     * @param serverConn IP del servidor de fallback
     */
    public static void setFallbackServer(String serverConn) {
        if ((fallbackServer != null) && (serverConn.equals("NOALT"))) {
            fallbackServer = null;
        } else if ((fallbackServer == null) && (!serverConn.equals("NOALT"))) {
            fallbackServer = serverConn;
        }
    }

    /**
     * Comenzar una nueva conexión con el servidor
     *
     * @param serverIp Dirección ip del servidor (Balanceador)
     */
    public static void startConnection(String serverIp) {
        // Crear el contexto
        context = new ZContext();

        // Crear el Socket y darle una identidad (UUID)
        System.out.println("\nConectando al servidor... " + serverIp);
        socket = context.createSocket(SocketType.REQ);
        socket.setIdentity(clientUUID.toString().getBytes(ZMQ.CHARSET));

        // Conectarse al servidor
        socket.connect("tcp://" + serverIp + ":" + PORT);
        System.out.println("Conectado a " + serverIp);
    }

    /**
     * Detener todas las conexiones
     */
    public static void stopConnection() {
        socket.close();
        context.close();
        context.destroy();
        System.err.println("Conexión detenida");
    }

    public static void main(String[] args) {
        // Iniciar el cliente
        System.out.println("Cliente - Gestión de productos");
        System.out.println("UUID del cliente: " + clientUUID);

        // Obtener datos del servidor
        System.out.print("Digite la dirección IP del Servidor: ");
        String serverIp = scanner.nextLine();

        // Inicializar la conexión
        startConnection(serverIp);

        // Conectarse al servicio de HealthCheck
        Thread healthCheck = new Thread(new BalancerHealthCheck(serverIp, context));

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
            } else if (e instanceof WorkerNotResponding) {
                if (consecutiveTries >= WORKER_CHECK_MAX_TRIES) {
                    System.err.println("No hay servidores disponibles, intenta más tarde");
                    System.exit(1);
                }

                System.err.println("Reiniciando petición (" + consecutiveTries + ")");
                fallbackReady.set(false);
                stopConnection();
                startConnection(serverIp);
                consecutiveTries++;
                fallbackReady.set(true);
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
            Thread workerCheck = new Thread(new WorkerCheck(serverIp, context));
            workerCheck.setUncaughtExceptionHandler(failedHealthCheck);

            if (!wasInterrupted) {
                System.out.println("\nSistema de gestión de productos:");
                System.out.println("1. Listar productos");
                System.out.println("2. Descripción del producto");
                System.out.println("3. Comprar un producto");
                System.out.println("4. PING a worker");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();
            }

            // Parsear la desición
            try {
                // Tomar un camino dependiendo de la opción
                if (fallbackReady.get()) {
                    // Seleccionar la opción
                    switch (opcion) {
                        default -> System.out.println("Opción desconocida");
                        case 0 -> System.out.println("Saliendo...");
                        case 1 -> {
                            System.out.println("Consultando lista de productos: ");
                        }
                        case 2 -> {
                            System.out.print("Digite el ID del producto a consultar:");
                            int idProducto = scanner.nextInt();
                            System.out.println("Consultando producto " + idProducto);
                        }
                        case 3 -> {
                            System.out.print("Digite el ID del producto a comprar");
                            int idProducto = scanner.nextInt();
                            System.out.println("Comprando producto " + idProducto);
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
            } catch (InterruptedException ignored) {
            }

        } while (opcion != 0);
    }
}