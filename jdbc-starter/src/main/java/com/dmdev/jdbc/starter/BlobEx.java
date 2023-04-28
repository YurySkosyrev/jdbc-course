package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.util.ConnectionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.*;

public class BlobEx {
    public static void main(String[] args) throws SQLException, IOException {

        getImage();
//        saveImage();
    }

    private static void getImage() throws SQLException, IOException {
        String sql = """
                   SELECT image 
                   FROM aircraft
                   WHERE id = ?
                   """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, 1);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                byte[] image = resultSet.getBytes("image");
                Files.write(Path.of("C:\\Java\\jdbc-course\\jdbc-starter\\src\\main\\resources\\", "boeingFromDb.jpg"),
                        image, StandardOpenOption.CREATE);
            }
        }
    }

    private static void saveImage() throws SQLException, IOException {

        String sql = """
                   UPDATE aircraft 
                   SET image = ?
                   WHERE id = 1
                   """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            preparedStatement.setBytes(1, Files.readAllBytes(
                    Path.of("C:\\Java\\jdbc-course\\jdbc-starter\\src\\main\\resources\\", "boeing.jpg")));
            preparedStatement.executeUpdate();
        }
    }

/*
    private static void saveImage() throws SQLException, IOException {

        String sql = """
                   UPDATE aircraft
                   SET image = ?
                   WHERE id = 1
                   """;

        try (Connection connection = ConnectionManager.open();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);
            Blob blob = connection.createBlob();
            blob.setBytes(1, Files.readAllBytes(Path.of("resources", "boeing777.jpg")));

            preparedStatement.setBlob(1, blob);
            preparedStatement.executeUpdate();

            connection.commit();
        }
    }
*/
}
