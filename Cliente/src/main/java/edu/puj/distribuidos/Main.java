package edu.puj.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    /* Puertos */
    public static final Integer PORT = 5550; // Puerto del Servidor
    public static final Integer HEALTH_CHECK_PORT = 5570; // Puerto de HEALTH_CHECK
    public static final Integer WORKER_CHECK_PORT_CLIENT = 5572; // Puerto de WORKER_CHECK
    public static final Integer WORKER_CHECK_MAX_TRIES = 5; // Puerto de WORKER_CHECK

    /* Otras variables */
    public static final Integer HEALTH_CHECK_TIMEOUT = 5000;
    public static final Integer WORKER_CHECK_TIMEOUT = 10000;

    public static void main(String[] args) {
        try {
            new DoRequest().main();
        } catch (IOException e) {
            System.err.println("No se pudo crear LOG");
        }
    }
}