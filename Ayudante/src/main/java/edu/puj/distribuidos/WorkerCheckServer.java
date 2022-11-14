package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.UUID;

public class WorkerCheckServer implements Runnable {

    private final ZMQ.Socket socket;
    private final UUID clientUUID;

    public WorkerCheckServer(ZContext context, UUID clientUUID, String serverIP) {
        // Puerto de servicio HealthCheck
        socket = context.createSocket(SocketType.PUB);
        socket.connect("tcp://" + serverIP + ":" + Main.WORKER_CHECK_PORT_SERVER);
        this.clientUUID = clientUUID;
        System.out.println("Servicio de WorkerCheck habilitado para: " + clientUUID);
        System.out.println("Servicio de WorkerCheck en el puerto: " + serverIP + ":" + Main.WORKER_CHECK_PORT_SERVER);
    }

    @Override
    public void run() {
        socket.send((clientUUID + " OK").getBytes(ZMQ.CHARSET));
    }
}
