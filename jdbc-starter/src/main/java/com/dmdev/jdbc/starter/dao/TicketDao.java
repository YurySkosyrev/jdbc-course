package com.dmdev.jdbc.starter.dao;

import com.dmdev.jdbc.starter.entity.Ticket;
import com.dmdev.jdbc.starter.exception.DaoException;
import com.dmdev.jdbc.starter.util.ConnectionPoolManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketDao {

    private static final TicketDao INSTANCE = new TicketDao();
    private static final String DELETE_SQL = """
            DELETE FROM ticket WHERE id = ?
            """;
    private static final String SAVE_SQL = """
            INSERT INTO ticket (passenger_no, passenger_name, flight_id, seat_no, cost) 
            VALUES (?, ?, ?, ?, ?);
            """;
    private static final String UPDATE_SQL = """
           UPDATE ticket 
           SET passenger_no = ?,
               passenger_name = ?,
               flight_id = ?,
               seat_no = ?,
               cost = ?
           WHERE id = ?;
            """;

    private static final String FIND_ALL_SQL = """
            SELECT id, passenger_no, passenger_name, flight_id, seat_no, cost 
            FROM ticket
            """;

    private static final String FIND_BY_ID_SQL = FIND_ALL_SQL + """
            WHERE id = ?;
            """;

    private TicketDao() {
    }

    public static TicketDao getInstance() {
        return INSTANCE;
    }

    public void update(Ticket ticket) {
        try(Connection connection = ConnectionPoolManager.get();
            PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL)) {

            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlightId());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());
            preparedStatement.setLong(6, ticket.getId());

            preparedStatement.executeUpdate();

        } catch (SQLException throwables) {
            throw  new DaoException(throwables);
        }
    }

    
    public List<Ticket> findAll() {
        
        List<Ticket> tickets = new ArrayList<>();

        try (Connection connection = ConnectionPoolManager.get();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_ALL_SQL)
        ) {

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }
            return tickets;
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }
    
    public Optional<Ticket> findById(Long id) {
        Ticket ticket = null;

        try(Connection connection = ConnectionPoolManager.get();
            PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            preparedStatement.setLong(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ticket = buildTicket(resultSet);
            }

            return Optional.ofNullable(ticket);

        } catch (SQLException throwables) {
            throw  new DaoException(throwables);
        }
    }

    public Ticket save(Ticket ticket) {
        try(Connection connection = ConnectionPoolManager.get();
            PreparedStatement preparedStatement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, ticket.getPassengerNo());
            preparedStatement.setString(2, ticket.getPassengerName());
            preparedStatement.setLong(3, ticket.getFlightId());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());

            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                ticket.setId(generatedKeys.getLong("id"));
            }

            return ticket;

        } catch (SQLException throwables) {
            throw  new DaoException(throwables);
        }
    }

    public boolean delete(Long id) {
        try(Connection connection = ConnectionPoolManager.get();
            PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL)) {

            preparedStatement.setLong(1, id);
            return preparedStatement.executeUpdate() > 0;

        } catch (SQLException throwables) {
            throw  new DaoException(throwables);
        }
    }

    private Ticket buildTicket(ResultSet resultSet) throws SQLException {
        Ticket ticket;
        ticket = new Ticket();

        ticket.setId(resultSet.getLong("id"));
        ticket.setPassengerNo(resultSet.getString("passenger_no"));
        ticket.setPassengerName(resultSet.getString("passenger_name"));
        ticket.setFlightId(resultSet.getLong("flight_id"));
        ticket.setSeatNo(resultSet.getString("seat_no"));
        ticket.setCost(resultSet.getBigDecimal("cost"));
        return ticket;
    }

}
