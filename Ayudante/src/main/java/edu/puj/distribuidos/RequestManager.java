package edu.puj.distribuidos;

import org.zeromq.*;

import java.util.UUID;

public class RequestManager extends Thread {
    protected Integer nRequest = 0; // Contador de la cantidad de solicitudes atendidas
    protected final UUID workerUUID = UUID.randomUUID();
    protected final String serverIP;
    protected final ZContext context;

    public RequestManager(String serverIP, ZContext context) {
        this.serverIP = serverIP;
        this.context = context;
    }

    @Override
    public void run() {
        // Inicializar RED
        try (ZMQ.Socket socket = context.createSocket(SocketType.REP)) {
            // Crear el Socket y darle una identidad (UUID)
            System.out.println("Conectando al servidor... " + serverIP);
            socket.setIdentity(workerUUID.toString().getBytes(ZMQ.CHARSET));
            socket.setImmediate(true);

            // Conectarse al servidor
            socket.connect("tcp://" + serverIP + ":" + Main.PORT);
            System.out.println("UUID del worker: " + workerUUID);

            // Bucle infinito
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Recibir solicitud
                    System.out.println("\n(" + nRequest + ") Esperando solicitudes");
                    String request = new String(socket.recv(), ZMQ.CHARSET);
                    System.out.println("Solicitud: " + request);

                    // Procesar solicitud
                    String respuesta = switch (request) {
                        case "PING" -> "PONG";
                        default -> "FAIL";
                    };

                    // Responder solicitud
                    System.out.println("Enviando respuesta...");
                    socket.send(respuesta.getBytes(ZMQ.CHARSET));
                    nRequest++;

                } catch (ZMQException e) {
                    switch (ZMQ.Error.findByCode(e.getErrorCode())) {
                        case ETERM -> System.out.println("Sistema detenido");
                        case EAGAIN -> System.out.println("El Balanceador no responde a las solicitudes");
                        default -> System.out.println("Error de conexión en el sistema (" + e.getMessage() + ")");
                    }
                }
            }
        } finally {
            // Mostrar cierre
            System.out.println("\nSistema Finalizado (" + workerUUID + ")");
            System.out.println("Se atendieron " + nRequest + " solicitudes");
        }
    }
}
