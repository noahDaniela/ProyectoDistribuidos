package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Main {

    static final Integer PORT = 5555;

    public static void main(String[] args) {
        System.out.println("Balanceador de cargas");

        // Creando la red de ZMQ
        try (ZContext context = new ZContext()) {

            // Creando el servidor
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:" + PORT.toString());
            System.out.println("Iniciado el servidor en el puerto: " + PORT.toString());

            // Bucle infinito
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir la petición
                byte[] request = socket.recv();
                System.out.println("Recibida petición de un cliente");

                // Mostrar la petición
                String requestStr = new String(request, ZMQ.CHARSET);
                System.out.println("Petición: " + requestStr);

                // Responder
                String response = " Mundo!";
                socket.send(response.getBytes(ZMQ.CHARSET));
                System.out.println("Enviada respuesta: " + response);
            }
        }
    }
}