package edu.puj.distribuidos;

import org.zeromq.ZContext;

import java.util.Scanner;

public class Main {
    public static final Integer PORT = 5560; // Puerto del Servidor
    private static final Scanner scanner = new Scanner(System.in); // Scanner para los menús

    private static Thread mainThread;

    public static void main(String[] args) {
        // Obtener datos del servidor
        System.out.println("Worker - Gestión de productos");
        System.out.print("Digite la dirección IP del Servidor: ");
        String serverIP = scanner.nextLine();

        ZContext context = new ZContext();

        // Crear el hilo principal
        mainThread = new RequestManager(serverIP, context);
        mainThread.start();

        // Agregar HOOK para cuando se quiera salir
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Cerrar el contexto
                context.close();
                mainThread.interrupt();
                mainThread.join();

                // Destruir recursos
                context.destroy();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}