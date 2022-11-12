package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 * Escuchar las solicitudes del Balanceador Alternative
 */
public class AlternativeCheckServer implements Runnable {

    private final ZMQ.Socket socket;

    public AlternativeCheckServer(ZContext context) {
        // Abrir el socket
        socket = context.createSocket(SocketType.SUB);
        socket.bind("tcp://*:" + Main.ALTPING_PORT);
        System.out.println("Servicio de Balanceador alterno en el puerto: " + Main.ALTPING_PORT);

        // Suscribirse
        socket.subscribe("ALTPING".getBytes(ZMQ.CHARSET));
        socket.setReceiveTimeOut(Main.ALTERNATIVE_CHECK_TIMEOUT);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Parse response
                byte[] response = socket.recv();
                if (response == null) throw new ZMQException(socket.errno());
                String[] data = new String(response, ZMQ.CHARSET).split("\\s+");
                // Guardar el servidor
                if (Main.alternativeIP == null) System.out.println("Servidor alternativo conectado: " + data[1]);
                Main.alternativeIP = data[1];
            } catch (ZMQException e) {
                // No hay respuesta de los alternativos
                if (Main.alternativeIP != null) System.err.println("Servidor alternativo no responde");
                Main.alternativeIP = null;
            }
        }
    }
}
