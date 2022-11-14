package edu.puj.distribuidos;

import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConection {
    String host;
    String puerto;
    String servicio;
    String password;
    String user;

    DatabaseConection(String host, int puerto, String db, String user, String password) {
        this.host = host;
        this.puerto = Integer.toString(puerto);
        this.servicio = db;
        this.user = user;
        this.password = password;
    }

    public java.sql.Connection conectarBD() {
        java.sql.Connection connection = null;
        try {
            String connectionString = createConnectionString(host, puerto, servicio);
            connection = DriverManager.getConnection(connectionString, user, password);
        } catch (SQLException e) {
            System.out.println("Error: " + e);
        }
        return connection;
    }

    private String createConnectionString(String host, String puerto, String servicio) {
        return "jdbc:postgresql://" + host + ":" + puerto + "/" + servicio + "?useServerPrepStmts=true";
    }


}
