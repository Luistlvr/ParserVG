/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.db;

import com.bg.parser.html.helper.NaturaGlobals;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class MySql implements Database {
    private static MySql instance = null;
    private Connection connection;
    private Statement SqlStatement;
    
    private MySql() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(NaturaGlobals.DB_URL + NaturaGlobals.DB_NAME, 
                    NaturaGlobals.DB_USERNAME, NaturaGlobals.DB_PASSWORD);
        } catch (SQLException ex) {
            Logger.getLogger(MySql.class.getName()).log(Level.SEVERE, null, ex);
        } catch(ClassNotFoundException e) {
            System.out.println("Error en el Driver: " + e.getMessage());
        } 
    }

    /**
     * getConnection: Creates a new connection
     * @return A MySql object
     */
    public static MySql getConnection() {
        if(instance == null) 
            instance = new MySql();
        return instance;
    }
    
    @Override
    public int executeStatement(String pSqlQuery) throws SQLException {  
        int insertedKeyValue = -1;
        if(connection != null) {
            SqlStatement = connection.createStatement();
            SqlStatement.executeUpdate(pSqlQuery);

            ResultSet rs = SqlStatement.getGeneratedKeys();
            if(rs.next())
                insertedKeyValue = rs.getInt(1);
            return insertedKeyValue;
        }
        return insertedKeyValue;
    }
    
    @Override
    public ResultSet executeQuery(String pSqlQuery) throws SQLException {
        if(connection != null) {
            SqlStatement = connection.createStatement();
            ResultSet Result = SqlStatement.executeQuery(pSqlQuery);

            return Result;
        }
        return null;
     }
    
    @Override
    public void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            System.out.println("Error al cerrar la conexión.");
        }
    }
}
