package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrepareStatementEx {
    public static void main(String[] args) throws SQLException {

        LocalDateTime time1 = LocalDate.of(2020,1,1).atStartOfDay();
        LocalDateTime time2 = LocalDate.of(2021,10,1).atStartOfDay();

        System.out.println(selectIdBetween(time1, time2));

    }

    private static List<Long> selectIdBetween(LocalDateTime time1, LocalDateTime time2) throws SQLException {

        List<Long> result = new ArrayList<>();

        String sql = """
                SELECT id FROM flight
                WHERE departure_date BETWEEN ? AND ?;
                """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setTimestamp(1, Timestamp.valueOf(time1));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(time2));

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getLong("id"));
            }
        }

        return result;
    }
}
