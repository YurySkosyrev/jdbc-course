package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionEx {
    public static void main(String[] args) throws SQLException {

        Long flightId = 8L;

        deleteFromFlight(flightId);

    }

    private static void deleteFromFlight(Long flightId) throws SQLException {

        String deleteFromTicketSql = "DELETE FROM ticket WHERE flight_id = ?";
        String deleteFromFlightSql = "DELETE FROM flight WHERE id = ?";

        Connection connection = null;
        PreparedStatement deleteTicketStatement = null;
        PreparedStatement deleteFlightStatement = null;

        try {
            connection = ConnectionManager.open();
            deleteTicketStatement = connection.prepareStatement(deleteFromTicketSql);
            deleteFlightStatement = connection.prepareStatement(deleteFromFlightSql);

            connection.setAutoCommit(false);

            deleteTicketStatement.setLong(1, flightId);
            deleteFlightStatement.setLong(1, flightId);

            deleteTicketStatement.executeUpdate();

            if (true) {
                throw new RuntimeException("Ooops");
            }

            deleteFlightStatement.executeUpdate();

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
            if (deleteFlightStatement != null) {
                deleteFlightStatement.close();
            }
            if (deleteTicketStatement != null) {
                deleteTicketStatement.close();
            }
        }
    }
}
