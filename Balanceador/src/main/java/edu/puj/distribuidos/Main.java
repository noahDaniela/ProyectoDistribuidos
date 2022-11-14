package edu.puj.distribuidos;

import org.zeromq.*;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    /* Puertos */
    public static final Integer CLIENT_PORT = 5550;
    public static final Integer WORKER_PORT = 5560;
    public static final Integer HEALTH_CHECK_PORT = 5570;
    public static final Integer ALTPING_PORT = 5571;
    public static final Integer WORKER_CHECK_PORT_CLIENT = 5572;
    public static final Integer WORKER_CHECK_PORT_SERVER = 5573;

    /* Otras constantes */
    public static final Integer SERVER_HEALTH_CHECK_TIME = 1000;
    public static final Integer HEALTH_CHECK_TIMEOUT = 5000; // Tiempo de espera antes de dar por muerto al Servidor
    public static final Integer ALTERNATIVE_CHECK_TIMEOUT = 5000;

    /* Direcciones IP */
    public static String alternativeIP;

    public static void main(String[] args) {
        // Mostrar el menú
        System.out.println("\nBalanceador de cargas - Gestión de productos");
        System.out.println("Seleccione un modo de operación: ");
        System.out.println("1. MAIN - Principal");
        System.out.println("2. ALTERNATIVE - Alternativo");
        System.out.println("0. Cancelar");
        System.out.print("Seleccione el modo de operación: ");

        // Obtener modo de operación
        Scanner scanner = new Scanner(System.in);
        int opcion = Integer.parseInt(scanner.nextLine());

        // Variables de operación
        boolean operateAsMain = opcion == 1;
        String mainBalancer = "localhost";
        String alternativeBalancer = "localhost";

        switch (opcion) {
            default -> System.out.println("Opción inválida");
            case 1 -> System.out.println("Iniciando Principal\n");
            case 2 -> {
                System.out.print("\nDigite la IP del Balanceador (MAIN) Principal: ");
                mainBalancer = scanner.nextLine();
                System.out.print("Digite la IP del Balanceador (ALTERNATIVE) Alternativo: ");
                alternativeBalancer = scanner.nextLine();
            }
            case 0 -> {
                System.out.println("Saliendo...");
                System.exit(0);
            }
        }

        // Manejador de eventos
        ScheduledExecutorService checkers = Executors.newSingleThreadScheduledExecutor();

        // Crear el contexto
        ZContext context = new ZContext();

        // Creando el proxy de ZMQ
        try {
            /* Modo Alternativo - ALTERNATIVE */
            if (!operateAsMain) {
                // Abrir el Socket del modo alternativo
                System.out.println("Conectándose al balanceador " + mainBalancer);
                ZMQ.Socket alternative = context.createSocket(SocketType.PUB);
                alternative.connect("tcp://" + mainBalancer + ":" + ALTPING_PORT);

                // Habilitar HealthCheck (Hasta que el MAIN muera)
                AtomicReference<Boolean> waitingForMain = new AtomicReference<>(true);
                Thread healthCheck = new Thread(new BalancerHealthCheck(mainBalancer, context));
                healthCheck.setUncaughtExceptionHandler((t, e) -> {
                    if (e instanceof ServerNotResponding) {
                        waitingForMain.set(false);
                        System.err.println("\nServidor no responde, asumiendo responsabilidad como Balanceador principal");
                    }
                });
                healthCheck.start();

                // Enviar ALTPING cada 5s
                while (waitingForMain.get()) {
                    alternative.send(("ALTPING " + alternativeBalancer).getBytes(ZMQ.CHARSET));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // Destruir el contexto
                alternative.close();
                healthCheck.interrupt();
                context.close();
                context.destroy();
                healthCheck.join();

                // Crar el nuevo contexto
                context = new ZContext();
            }

            /* Modo Principal - MAIN */

            // Configurar checkeo de balanceador alternativo
            Thread alternativeCheck = new Thread(new AlternativeHealthCheck(context));

            // Configurar checkeo de salud
            checkers.scheduleAtFixedRate(new HealthCheckServer(context),
                    0, SERVER_HEALTH_CHECK_TIME, TimeUnit.MILLISECONDS);

            // Creando el servidor que recibe solicitudes del cliente
            ZMQ.Socket clients = context.createSocket(SocketType.ROUTER);
            clients.bind("tcp://*:" + CLIENT_PORT);
            System.out.println("Servicio de clientes por el puerto: " + CLIENT_PORT);

            // Creando el socket que atiende a los workers
            ZMQ.Socket workers = context.createSocket(SocketType.DEALER);
            workers.bind("tcp://*:" + WORKER_PORT);
            System.out.println("Servicio de worker en el puerto: " + WORKER_PORT);

            // Creando sockets del WORKERCHECK
            ZMQ.Socket workerCheckPublisher = context.createSocket(SocketType.XPUB);
            ZMQ.Socket workerCheckSubscriber = context.createSocket(SocketType.XSUB);

            workerCheckSubscriber.bind("tcp://*:" + WORKER_CHECK_PORT_SERVER); // Workers
            System.out.println("Servicio de WorkerCheck (Worker) en el puerto: " + WORKER_CHECK_PORT_SERVER);
            workerCheckPublisher.bind("tcp://*:" + WORKER_CHECK_PORT_CLIENT); // Clients
            System.out.println("Servicio de WorkerCheck (Client) en el puerto: " + WORKER_CHECK_PORT_CLIENT);

            // Iniciar Check de alternativos
            alternativeCheck.start();

            // Crear el hilo proxy de alternativos
            Thread workerThread = new Thread() {
                @Override
                public void run() {
                    ZMQ.proxy(workerCheckSubscriber, workerCheckPublisher, null);
                    super.run();
                }
            };

            workerThread.start();

            // Crear thread de proxy
            ZMQ.proxy(clients, workers, null);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            context.close();
            context.destroy();
        }
    }
}