package edu.puj;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZSocket;

import java.util.Scanner;
import java.util.UUID;

public class Main {

    static final Integer PORT = 5555; // Puerto del Servidor
    static final UUID clientUUID = UUID.randomUUID(); // UUID identificador del cliente
    static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

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
                System.out.println("1. Hola");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();

                // Tomar un camino dependiendo de la opción
                switch (opcion) {
                    case 1 -> {
                        System.out.println("Diciendo hola al servidor");

                        // Enviar información al servidor
                        String request = "Hola";

                        // Enviar una petición
                        socket.send(request.getBytes(ZMQ.CHARSET));

                        // Recibir una petición
                        byte[] response = socket.recv();
                        String responseStr = new String(response, ZMQ.CHARSET);

                        // Mostrar la respuesta
                        System.out.println("Respuesta del Servidor: " + responseStr);
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