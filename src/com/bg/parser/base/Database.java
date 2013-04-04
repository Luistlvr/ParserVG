/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.base;

import java.sql.*;

/**
 *
 * @author luis
 */
public abstract interface Database {
    /**
     * executeStatement: executes an update to the database
     * @param pSqlQuery
     * @return true "succesful, false failed
     */
    abstract public int executeStatement(String pSqlQuery) throws SQLException;
    
    /**
     * ejecuteQuery: executes a query to the database
     * @param pSqlQuery
     * @return
     */
    abstract public ResultSet executeQuery(String pSqlQuery) throws SQLException;
    
    /**
     * closeConnection: close the actual connection
     */
    abstract public void closeConnection();
}
