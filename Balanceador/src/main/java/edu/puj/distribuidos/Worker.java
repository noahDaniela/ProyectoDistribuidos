package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Worker extends Thread {
    private final ZContext context; // Contexto de RED
    private ZMQ.Socket receptor; // Por donde recibe las solicitudes
    private final String TAG = "Worker (" + this.getName() + "): ";

    public Worker(ZContext context) {
        this.context = context;
    }

    @Override
    public synchronized void start() {
        // Crear el receptor de red
        receptor = context.createSocket(SocketType.REP);
        receptor.connect(Main.WORKERS_PATH);
        System.out.println(TAG + "Receptor inicializado");

        // Iniciar el hilo
        super.start();
    }

    @Override
    public void run() {
        super.run();
        // Ejecutar infinitamente hasta que alguien lo interrumpa
        while (!Thread.currentThread().isInterrupted()) {
            // Recibir una solicitud
            System.out.println(TAG + "Esperando solicitud");
            byte[] request = receptor.recv();
            System.out.println(TAG + "Recibido -> " + new String(request, ZMQ.CHARSET));

            // Enviar una respuesta
            System.out.println(TAG + "Enviando respuesta");
            receptor.send("OK".getBytes(ZMQ.CHARSET));
        }
    }
}
