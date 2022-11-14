package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class WorkerCheck implements Runnable {
    protected final ZMQ.Socket socket;

    public WorkerCheck(String serverIP, ZContext context) {
        // Inicializar el socket
        this.socket = context.createSocket(SocketType.SUB);
        socket.connect("tcp://" + serverIP + ":" + Main.WORKER_CHECK_PORT_CLIENT);

        // Suscribirse a su propio UUID
        socket.subscribe((Main.clientUUID.toString()).getBytes(ZMQ.CHARSET));
        socket.setReceiveTimeOut(Main.HEALTH_CHECK_TIMEOUT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Servicio de WorkerCheck activado");
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir la solicitud
                byte[] response = socket.recv();
                if (response == null && !Thread.currentThread().isInterrupted())
                    throw new ZMQException(socket.errno());
            }
        } catch (ZMQException e) {
            if (ZMQ.Error.findByCode(e.getErrorCode()) == ZMQ.Error.EAGAIN) {
                System.err.println("\nEl Trabajador no responde");
                throw new WorkerNotResponding();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deconocido, deteniendo el Worker");
            System.exit(1);
        }
    }
}
