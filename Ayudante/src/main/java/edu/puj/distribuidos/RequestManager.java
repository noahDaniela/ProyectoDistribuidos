package edu.puj.distribuidos;

import org.zeromq.*;

import java.util.UUID;

public class RequestManager implements Runnable {
    protected Integer nRequest = 0; // Contador de la cantidad de solicitudes atendidas
    protected final UUID workerUUID = UUID.randomUUID();
    protected final String serverIP;

    public RequestManager(String serverIP) {
        this.serverIP = serverIP;
    }

    public String ping() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "PONG";
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
                    String request = new String(socket.recv(), ZMQ.CHARSET);
                    System.out.println("Solicitud: " + request);

                    // Procesar solicitud
                    String respuesta = switch (request) {
                        case "PING" -> ping();
                        default -> "FAIL";
                    };

                    // Responder solicitud
                    System.out.println("Enviando respuesta...");
                    socket.send(respuesta.getBytes(ZMQ.CHARSET));
                    nRequest++;

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
        }
    }
}
