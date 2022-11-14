package edu.puj.distribuidos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class DatabaseQuery {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    public DatabaseQuery(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String consultarProductos() {
        StringBuilder returnVal = new StringBuilder();
        try {
            DatabaseConection conexion = new DatabaseConection(host, port, database, username, password);
            Connection connection = conexion.conectarBD();
            PreparedStatement st = null;
            ResultSet rs = null;
            String sql = "select * from productos";
            st = conexion.conectarBD().prepareStatement(sql);
            rs = st.executeQuery();
            returnVal.append("ID" + "\t" + "Nombre" +
                                     "\t" + "Cantidad" + "\t" + "Precio\n");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                Double precio = rs.getDouble("Precio");
                returnVal.append(id + "\t" + nombre +
                                         "\t" + cantidad + "\t" + precio + "\n");
            }
        } catch (Exception e) {
            return e.getMessage();
        }

        return returnVal.toString();
    }

    public String consultarProducto(Integer id) {
        DatabaseConection conexion = new DatabaseConection(host, port, database, username, password);
        Connection connection = conexion.conectarBD();
        PreparedStatement st = null;
        ResultSet rs = null;
        String nombre = " ";
        Integer cantidad = 0;
        Double precio = 0d;
        try {
            String sql = "select nombre, cantidad, precio from productos where id = (?)";
            st = conexion.conectarBD().prepareStatement(sql);
            st.setInt(1, id);
            rs = st.executeQuery();
            while (rs.next()) {
                nombre = rs.getString(1);
                cantidad = rs.getInt(2);
                precio = rs.getDouble(3);
            }
            return ("Producto: " + nombre + " \n" + "Cantidad disponible: " + cantidad + "\n" + "Precio: " + precio);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String adquirirProducto(Integer id) {
        DatabaseConection conexion = new DatabaseConection(host, port, database, username, password);
        Connection connection = conexion.conectarBD();
        PreparedStatement st = null;
        PreparedStatement st2 = null;
        int cantidadProducto = 0;
        int cantidadActual = 0;
        ResultSet rs = null;
        ResultSet rs2 = null;
        try {
            String cantidad = "select cantidad from productos where id = (?)";
            st2 = conexion.conectarBD().prepareStatement(cantidad);
            st2.setInt(1, id);
            rs = st2.executeQuery();
            while (rs.next()) {
                cantidadProducto = rs.getInt(1);
            }
            cantidadActual = cantidadProducto - 1;
            String sql = "Update productos set cantidad = (?) where id = (?)";
            st = conexion.conectarBD().prepareStatement(sql);
            cantidadActual = cantidadProducto - 1;
            st.setInt(1, cantidadActual);
            st.setInt(2, id);
            st.executeUpdate();
            return "Producto adquirido correctamente";
        } catch (Exception e) {
            return e.getMessage();
        }

    }
}