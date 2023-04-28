package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class BatchEx {
    public static void main(String[] args) throws SQLException {

        Long flightId = 8L;

        deleteFromFlight(flightId);

    }

    private static void deleteFromFlight(Long flightId) throws SQLException {

        String deleteTicketSql = "DELETE FROM ticket WHERE flight_id = " + flightId;
        String deleteFlightSql = "DELETE FROM flight WHERE id = " + flightId;

        Connection connection = null;
        Statement statement = null;


        try {
            connection = ConnectionManager.open();
            statement = connection.createStatement();
            connection.setAutoCommit(false);

            statement.addBatch(deleteTicketSql);
            statement.addBatch(deleteFlightSql);

            statement.executeBatch();

            connection.commit();

        } catch (Exception e) {
           if (connection != null) {
               connection.rollback();
           }
           throw e;
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }
}
