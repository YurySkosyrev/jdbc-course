package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.Connection;
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


        try (Connection connection = ConnectionManager.open();
             Statement statement = connection.createStatement()) {
            System.out.println(connection.getTransactionIsolation());

            boolean executeResult = statement.execute(sql);
            System.out.println(executeResult);
        }

    }
}
