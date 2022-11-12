package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZSocket;

import java.util.Scanner;
import java.util.UUID;

public class Main {

    public static final Integer PORT = 5550; // Puerto del Servidor
    public static final UUID clientUUID = UUID.randomUUID(); // UUID identificador del cliente
    private static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

    public static void main(String[] args) {
        System.out.println("Cliente - Gestión de productos");

        // Obtener datos del servidor
        System.out.print("Digite la dirección IP del Servidor: ");
        String serverIp = scanner.nextLine();

        // Inicializar RED
        try (ZContext context = new ZContext()) {
            // Crear el Socket y darle una identidad (UUID)
            System.out.println("Conectando al servidor... " + serverIp);
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.setIdentity(clientUUID.toString().getBytes(ZMQ.CHARSET));

            // Conectarse al servidor
            socket.connect("tcp://" + serverIp + ":" + PORT.toString());
            System.out.println("UUID del cliente: " + clientUUID);

            // Mostrar el menú
            int opcion = 0;
            do {
                System.out.println("\nSistema de gestión de productos:");
                System.out.println("1. Listar productos");
                System.out.println("2. Descripción del producto");
                System.out.println("3. Comprar un producto");
                System.out.println("4. PING a worker");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();

                // Tomar un camino dependiendo de la opción
                switch (opcion) {
                    case 1 -> {
                        System.out.println("Consultando lista de productos: ");
                    }
                    case 2 -> {
                        System.out.print("Digite el ID del producto a consultar:");
                        int idProducto = scanner.nextInt();
                        System.out.println("Consultando producto " + idProducto);
                    }
                    case 3 -> {
                        System.out.print("Digite el ID del producto a comprar");
                        int idProducto = scanner.nextInt();
                        System.out.println("Comprando producto " + idProducto);
                    }
                    case 4 -> {
                        System.out.println("Realizando un ping...");

                        // Enviar una petición
                        socket.send("PING".getBytes(ZMQ.CHARSET));

                        // Recibir la respuesta
                        byte[] response = socket.recv();

                        // Mostrar la respuesta
                        System.out.println("Respuesta del servidor: " + new String(response, ZMQ.CHARSET));
                    }
                    case 0 -> System.out.println("Saliendo...");
                    default -> System.out.println("Opción desconocida");
                }
            } while (opcion != 0);

            // Desconectarse
            System.out.println("Desconectándose del sistema...");
            socket.close();
        }
    }
}