package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Servicio que responde a los HealthChecks de los Clientes y Workers
 */
public class HealthCheckServer implements Runnable {
    private final ZMQ.Socket socket;

    public HealthCheckServer(ZContext context) {
        // Puerto de servicio HealthCheck
        socket = context.createSocket(SocketType.PUB);
        socket.bind("tcp://*:" + Main.HEALTH_CHECK_PORT);
        System.out.println("Servicio de HealthCheck en el puerto: " + Main.HEALTH_CHECK_PORT);
    }

    @Override
    public void run() {
        // Enviar por el canal SCHECK
        if (Main.alternativeIP == null)
            socket.send("HCHECK NOALT".getBytes(ZMQ.CHARSET));
        else
            socket.send(("HCHECK " + Main.alternativeIP).getBytes(ZMQ.CHARSET));
    }
}
