package edu.puj.distribuidos;

import org.zeromq.*;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RequestManager implements Runnable {
    protected Integer nRequest = 0; // Contador de la cantidad de solicitudes atendidas
    protected Long totalTime = 0L; // Contador del tiempo total atendido
    protected final UUID workerUUID = UUID.randomUUID();
    protected final String serverIP;
    protected final DatabaseQuery database;

    public RequestManager(String serverIP, DatabaseQuery database) {
        this.serverIP = serverIP;
        this.database = database;
    }

    public String ping() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }
        return "PONG";
    }

    public String list() {
        return database.consultarProductos();
    }

    public String query(Integer id) {
        return database.consultarProducto(id);
    }

    public String buy(Integer id) {
        return database.adquirirProducto(id);
    }

    @Override
    public void run() {
        // Inicializar RED
        try (ZMQ.Socket socket = Main.getContext().createSocket(SocketType.REP)) {
            // Crear el Socket y darle una identidad (UUID)
            System.out.println("Conectando al servidor... " + serverIP);
            socket.setIdentity(workerUUID.toString().getBytes(ZMQ.CHARSET));

            // Conectarse al servidor
            socket.connect("tcp://" + serverIP + ":" + Main.PORT);
            System.out.println("UUID del worker: " + workerUUID);

            // Bucle infinito
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Recibir solicitud
                    System.out.println("\n(" + nRequest + ") Esperando solicitudes");
                    String client = new String(socket.recv(), ZMQ.CHARSET);
                    String requestUnparsed = new String(socket.recv(), ZMQ.CHARSET);
                    long startTime = System.currentTimeMillis();
                    System.out.println("Cliente: " + client);
                    System.out.println("Solicitud: " + requestUnparsed);

                    // Request
                    String[] request = requestUnparsed.split("\\s+");

                    // Iniciar WORKERCHECK
                    ScheduledExecutorService checkers = Executors.newSingleThreadScheduledExecutor();
                    Thread workerCheck = new Thread(new WorkerCheckServer(Main.getContext(), UUID.fromString(client), serverIP));
                    checkers.scheduleAtFixedRate(workerCheck,
                            0, Main.WORKER_CHECK_TIME, TimeUnit.MILLISECONDS);

                    // Procesar solicitud
                    String respuesta = switch (request[0]) {
                        case "PING" -> ping();
                        case "LIST" -> list();
                        case "QUERY" -> query(Integer.valueOf(request[1]));
                        case "BUY" -> buy(Integer.valueOf(request[1]));
                        default -> "FAIL";
                    };

                    // Responder solicitud
                    System.out.println("Enviando respuesta...");
                    socket.send(respuesta.getBytes(ZMQ.CHARSET));
                    long endTime = System.currentTimeMillis();
                    nRequest++;

                    // Detener WORKERCHECK
                    System.out.println("Solicitud procesada en: " + (endTime - startTime) + "ms");
                    totalTime += (endTime - startTime);
                    checkers.shutdownNow();

                } catch (ZMQException e) {
                    switch (ZMQ.Error.findByCode(e.getErrorCode())) {
                        case ETERM -> System.out.println("Contexto ZMQ finalizado");
                        case EAGAIN -> System.out.println("El Balanceador no responde a las solicitudes");
                        default -> System.out.println("Error de conexión en el sistema (" + e.getMessage() + ")");
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Mostrar cierre
            System.out.println("Sistema Finalizado (" + workerUUID + ")");
            System.out.println("Se atendieron " + nRequest + " solicitudes");
            System.out.println("Tiempo promedio de respuesta: " + (totalTime / nRequest) + "ms");
        }
    }
}
