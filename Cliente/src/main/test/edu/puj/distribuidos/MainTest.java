package edu.puj.distribuidos;

class MainTest {
    public static void main(String[] args) {
        Runnable mainRun = () -> {
            try {
                new DoRequest("localhost",10).main();
            } catch (Exception e) {
                System.err.println("No se pudo crear log file");
            }
        };

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(mainRun);
            thread.start();
        }
    }
}