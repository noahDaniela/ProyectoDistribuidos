package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

    /* Puertos */
    public static final Integer PORT = 5550; // Puerto del Servidor
    public static final Integer HEALTH_CHECK_PORT = 5570; // Puerto de HEALTH_CHECK

    /* Otras variables */
    public static final UUID clientUUID = UUID.randomUUID(); // UUID identificador del cliente
    public static final Integer HEALTH_CHECK_TIMEOUT = 5000;

    /* Variables para el control del fallback (Balanceador alternativo) */
    private static String fallbackServer = null;

    private static final AtomicBoolean fallbackReady = new AtomicBoolean(true);

    /* Contexto de red y sockets */
    private static ZContext context;
    private static ZMQ.Socket socket;

    /**
     * Establecer el servidor de fallback
     *
     * @param serverConn IP del servidor de fallback
     */
    public static void setFallbackServer(String serverConn) {
        if ((fallbackServer != null) && (serverConn.equals("NOALT"))) {
            fallbackServer = null;
            System.err.println("(HealthCheck) Balanceador alternativo desconectado");
        } else if ((fallbackServer == null) && (!serverConn.equals("NOALT"))) {
            fallbackServer = serverConn;
            System.out.println("(HealthCheck) Balanceador alternativo: " + serverConn);
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
    }

    /**
     * Detener todas las conexiones
     */
    public static void stopConnection() {
        socket.close();
        context.close();
        context.destroy();
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
        Thread healthCheck = new Thread(new HealthCheck(serverIp, context));

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
                    System.exit(1);
                }

                // Cambiar a servidor principal
                fallbackReady.set(false); // Lock (Mientras se logra realizar la conexión)
                System.err.println("Servidor no responde, cambiando al servidor de Fallback");
                stopConnection();
                startConnection(fallbackServer);
                fallbackReady.set(true); // Liberar la conexión
            }
        };

        // Iniciar el servicio de HealthCheck
        healthCheck.setUncaughtExceptionHandler(failedHealthCheck);
        healthCheck.start();

        // Handle para cuando el programa se acabe
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Desconectarse
            System.out.println("Desconectándose del sistema...");
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
                        case 4 -> {
                            System.out.println("Realizando un ping...");
                            // Enviar una petición
                            socket.send("PING".getBytes(ZMQ.CHARSET));
                            // Recibir la respuesta
                            byte[] response = socket.recv();
                            // Mostrar la respuesta
                            System.out.println("Respuesta del servidor: " + new String(response, ZMQ.CHARSET));
                        }
                    }

                    // La operación pudo completarse correctamente
                    wasInterrupted = false;
                }
            } catch (ZMQException ex) {
                // En caso de que haya error de conexión se activa el flag de interrupción
                wasInterrupted = true;
            }
        } while (opcion != 0);
    }
}