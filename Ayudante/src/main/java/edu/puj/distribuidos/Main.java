package edu.puj.distribuidos;

import org.zeromq.ZContext;

import java.util.Scanner;

public class Main {

    /* Variablesde uso interno */
    private static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

    private static ZContext context;

    private static String fallbackServer;

    /* Puertos de red */
    public static final Integer HEALTH_CHECK_TIMEOUT = 5000; // Tiempo de espera antes de dar por muerto al Servidor
    public static final Integer HEALTH_CHECK_PORT = 5570; // Puerto para healthchecl
    public static final Integer WORKER_CHECK_PORT_SERVER = 5573; // Puerto para healthcheck del worker
    public static final Integer WORKER_CHECK_TIME = 1000; // Tiempo en el que se manda un WORKER_CHECK
    public static final Integer PORT = 5560; // Puerto del Servidor

    /* Hilos */
    private static Thread mainThread; // Hilo principal
    private static Thread healthCheckThread; // Hilo de detección de HealthCheck
    private static Thread.UncaughtExceptionHandler threadExceptionHandler; // Manejador de excepciones de los hilos

    /* Funciones */

    public static ZContext getContext() {
        return context;
    }

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
     * Crear todo los hilos, configurarlos e iniciarlos
     *
     * @param serverIP IP del servidor con el que se configurarán los hilos
     */
    private static void startServices(String serverIP, DatabaseQuery database) {
        // Crear el contexto
        context = new ZContext();

        // Crear el hilo principal (Atiende solicitudes)
        Thread mainThread = new Thread(new RequestManager(serverIP, database));
        // Crear el hilo del serivicio de HealthCheck
        Thread healthCheck = new Thread(new BalancerHealthCheck(serverIP));

        // Inicialización de los servicios
        healthCheck.setUncaughtExceptionHandler(threadExceptionHandler);
        healthCheck.start();
        mainThread.start();
    }

    /**
     * Detener todos los hilos
     *
     * @throws InterruptedException Hilo interrumpido
     */
    private static void stopServices() throws InterruptedException {
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread.join();
            mainThread = null;
        }
        if (healthCheckThread != null) {
            healthCheckThread.interrupt();
            healthCheckThread.join();
            healthCheckThread = null;
        }

        // Cerrar el contexto
        context.close();
        // Destruir recursos
        context.destroy();
    }

    public static void main(String[] args) {
        // Obtener datos del servidor
        System.out.println("Worker - Gestión de productos");
        System.out.print("Digite la dirección IP del Balanceador: ");
        String serverIP = scanner.nextLine();

        // Obtener datos de la base de datos
        System.out.println("Base de datos:");
        System.out.print("Digite la dirección IP de la base de datos: ");
        String dbip = scanner.nextLine();

        System.out.print("Digite el puerto: ");
        Integer puerto = Integer.valueOf(scanner.nextLine());

        System.out.print("Digite el nombre de la BD: ");
        String bdname = scanner.nextLine();

        System.out.print("Digite el nombre de usuario: ");
        String username = scanner.nextLine();

        System.out.print("Digite la constraseña: ");
        String password = scanner.nextLine();

        DatabaseQuery database = new DatabaseQuery(dbip, puerto, bdname, username, password);

        // Crear el manejador de excepciones
        threadExceptionHandler = (t, e) -> {
            if (e instanceof ServerNotResponding) {
                // Detener los hilos
                try {
                    stopServices();
                } catch (InterruptedException ex) {
                    System.err.println("Hilos detenidos forzosamente");
                }

                if (fallbackServer != null) {
                    // Obtener la dirección IP del ALTERNATIVE
                    System.out.println("\nCambiando a servidor alterno: " + fallbackServer);
                    // Reconfigurar los hilos
                    startServices(fallbackServer, database);
                    fallbackServer = null;

                } else {
                    System.err.println("No hay servidor alternativo...");
                    System.exit(1);
                }

            } else {
                System.err.println("Error desconocido");
                System.exit(1);
            }
        };

        // Habilitar los servicios
        startServices(serverIP, database);

        // Agregar HOOK para cuando se quiera salir
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Detener los servicios
                stopServices();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}