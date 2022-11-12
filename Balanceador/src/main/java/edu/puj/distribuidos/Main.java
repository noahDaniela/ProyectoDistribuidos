package edu.puj.distribuidos;

import org.zeromq.*;

public class Main {
    public static final Integer CLIENT_PORT = 5550;
    public static final Integer WORKER_PORT = 5560;

    public static void main(String[] args) {
        System.out.println("Balanceador de cargas - Gestión de productos");

        // Creando el proxy de ZMQ
        try (ZContext context = new ZContext()) {
            // Creando el servidor que recibe solicitudes del cliente
            ZMQ.Socket clients = context.createSocket(SocketType.ROUTER);
            clients.bind("tcp://*:" + CLIENT_PORT);
            System.out.println("Servicio de clientes por el puerto: " + CLIENT_PORT);

            // Creando el socket que atiende a los workers
            ZMQ.Socket workers = context.createSocket(SocketType.DEALER);
            workers.bind("tcp://*:" + WORKER_PORT);
            System.out.println("Servicio de worker en el puerto: " + WORKER_PORT);

            // Crear el proxy entre solicitudes y workers
            ZMQ.proxy(clients, workers, null);
        }
    }
}