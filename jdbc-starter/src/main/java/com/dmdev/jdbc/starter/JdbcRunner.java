package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcRunner {
    public static void main(String[] args) throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS info (
            id SERIAL PRIMARY KEY ,
            date TEXT NOT NULL 
            );
            """;

        String insertSql = """
            INSERT INTO info (date)
            VALUES 
            ('Test1'),
            ('Test2'),
            ('Test3'),
            ('Test4')
            ;
            """;

        String updateSql = """
                UPDATE info
                SET date = 'TestTest'
                WHERE id = 5
                RETURNING *
                """;

        String selectSql = """
                SELECT *
                FROM ticket;
                """;

        try (Connection connection = ConnectionManager.open();
             Statement statement = connection.createStatement()) {
            System.out.println(connection.getSchema());
            System.out.println(connection.getTransactionIsolation());

            ResultSet executeResult = statement.executeQuery(selectSql);
            while (executeResult.next()) {
                System.out.println(executeResult.getLong("id") + " " +
                        executeResult.getString("passenger_no"));

            }
//            System.out.println(executeResult);
//            System.out.println(statement.getUpdateCount());
        }

    }
}
