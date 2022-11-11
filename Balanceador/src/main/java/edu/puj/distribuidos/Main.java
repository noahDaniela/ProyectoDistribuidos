package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;

public class Main {

    /* --- Privado --- */
    private static final Integer N_WORKERS = 1;
    private static ArrayList<Thread> workerThreads = new ArrayList<>(); // Hilos trabajadores;

    /* --- Público --- */
    public static final Integer PORT = 5555;
    public static final String WORKERS_PATH = "inproc://workers";

    public static void main(String[] args) {
        System.out.println("Balanceador de cargas - Gestión de productos");

        // Creando la red de ZMQ
        try (ZContext context = new ZContext()) {
            // Creando el servidor que recibe solicitudes
            ZMQ.Socket clients = context.createSocket(SocketType.ROUTER);
            clients.bind("tcp://*:" + PORT);
            System.out.println("Iniciado el servidor en el puerto: " + PORT);

            // Creando los servicios que atenderán las solicitudes
            ZMQ.Socket workers = context.createSocket(SocketType.DEALER);
            workers.bind(Main.WORKERS_PATH);

            // Iniciar los workers
            System.out.println("Generando " + N_WORKERS + " workers...");
            for (int i = 0; i < N_WORKERS; i++) {
                Thread newWorker = new Worker(context);
                newWorker.start();
                workerThreads.add(newWorker);
            }

            // Crear el proxy entre solicitudes y workers
            ZMQ.proxy(clients, workers, null);
        }
    }
}