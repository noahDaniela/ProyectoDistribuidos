package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class HealthCheck implements Runnable {
    protected final ZMQ.Socket socket;

    public HealthCheck(String serverIP, ZContext context) {
        // Inicializar el socket
        this.socket = context.createSocket(SocketType.SUB);
        socket.connect("tcp://" + serverIP + ":" + Main.HEALTH_CHECK_PORT);
        System.out.println("Servicio de HealthCheck activado: " + serverIP);

        // Suscribirse a SCHECK y SALTERNATIVE
        socket.subscribe("HCHECK".getBytes(ZMQ.CHARSET));
        socket.setReceiveTimeOut(Main.HEALTH_CHECK_TIMEOUT);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir la solicitud
                byte[] response = socket.recv();
                if (response == null) throw new ZMQException(socket.errno());

                // Parsear la solicitud
                String[] data = new String(response, ZMQ.CHARSET).split("\\s+");
                Main.setFallbackServer(data[1]);
            }
        } catch (ZMQException e) {
            if (ZMQ.Error.findByCode(e.getErrorCode()) == ZMQ.Error.EAGAIN) {
                System.err.println("\nEl Servidor no responde");
                throw new ServerNotResponding();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error deconocido, deteniendo el Worker");
            System.exit(1);
        }
    }
}
