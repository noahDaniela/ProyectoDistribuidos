package edu.puj;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZSocket;

import java.util.Scanner;

public class Main {

    static final Integer PORT = 5555;
    static final Scanner scanner = new Scanner(System.in);


    public static void main(String[] args) {
        System.out.println("Cliente");

        // Obtener datos del servidor
        System.out.print("Digite la dirección IP del Servidor: ");
        String serverIp = scanner.nextLine();

        // Inicializar RED
        try (ZContext context = new ZContext()) {

            // Conectarse al servidor
            System.out.println("Conectando al servidor... " + serverIp);
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://" + serverIp + ":" + PORT.toString());

            // Mostrar el menú
            int opcion = 0;
            do {
                System.out.println("\nSistema de gestión de productos:");
                System.out.println("1. Hola");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = scanner.nextInt();

                switch (opcion) {
                    case 1:
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
                        break;

                    case 0:
                        System.out.println("Saliendo...");
                        break;

                    default:
                        System.out.println("Opción desconocida");
                        break;
                }

            } while (opcion != 0);

            // Desconectarse
            System.out.println("Desconectándose del sistema...");
            socket.close();

        }
    }
}