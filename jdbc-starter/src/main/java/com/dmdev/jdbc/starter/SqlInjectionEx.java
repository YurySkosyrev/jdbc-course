package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlInjectionEx {
    public static void main(String[] args) throws SQLException {

        String sqlParametr = "2 OR 1 = 1";
        System.out.println(extracted(sqlParametr));
    }

    private static List<Long> extracted(String sqlParametr) throws SQLException {
        String sqlInj = """
                SELECT id FROM ticket
                WHERE flight_id = %s
                """.formatted(sqlParametr);

        List<Long> result = new ArrayList<>();

        try (Connection connection = ConnectionManager.open();
                Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sqlInj);

            while (resultSet.next()) {
                result.add(resultSet.getObject("id", Long.class));
            }
        }
        return result;
    }
}
