package com.dmdev.jdbc.starter.dao;

import com.dmdev.jdbc.starter.dto.TicketFilter;
import com.dmdev.jdbc.starter.entity.Flight;
import com.dmdev.jdbc.starter.entity.Ticket;
import com.dmdev.jdbc.starter.exception.DaoException;
import com.dmdev.jdbc.starter.util.ConnectionPoolManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.*;

public class TicketDao implements Dao<Long, Ticket> {

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
            SELECT ticket.id, 
                passenger_no, 
                passenger_name, 
                flight_id, 
                seat_no, 
                cost,
                f.status,
                f.aircraft_id,
                f.arrival_airport_code,
                f.arrival_date,
                f.departure_airport_code,
                f.departure_date, 
                f.flight_no
            FROM ticket
            JOIN flight f 
            ON f.id = ticket.flight_id
            """;

    private static final String FIND_BY_ID_SQL = FIND_ALL_SQL + """
            WHERE ticket.id = ?;
            """;

    private final FlightDao flightDao = FlightDao.getInstance();

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
            preparedStatement.setLong(3, ticket.getFlight().id());
            preparedStatement.setString(4, ticket.getSeatNo());
            preparedStatement.setBigDecimal(5, ticket.getCost());
            preparedStatement.setLong(6, ticket.getId());

            preparedStatement.executeUpdate();

        } catch (SQLException throwables) {
            throw  new DaoException(throwables);
        }
    }

    public List<Ticket> findALl(TicketFilter filter) {

        List<Object> parametrs = new ArrayList<>();
        List<String> whereSql = new ArrayList<>();
        if (filter.seatNo() != null) {
            whereSql.add("seat_no LIKE ?");
            parametrs.add("%" + filter.seatNo() + "%");
        }
        if (filter.passengerName() != null) {
            whereSql.add("passenger_name = ?");
            parametrs.add(filter.passengerName());
        }
        parametrs.add(filter.limit());
        parametrs.add(filter.offset());

        String where = whereSql.stream()
                .collect(joining(" AND ", " WHERE ", " LIMIT ? OFFSET ? "));

        String sql = FIND_ALL_SQL + where;

        try (Connection connection = ConnectionPoolManager.get();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < parametrs.size(); i++) {
                preparedStatement.setObject(i + 1, parametrs.get(i));
            }
            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            List<Ticket> tickets = new ArrayList<>();

            while (resultSet.next()) {
                tickets.add(buildTicket(resultSet));
            }
            return tickets;

        } catch (SQLException e) {
            throw new DaoException(e);
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
            preparedStatement.setLong(3, ticket.getFlight().id());
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

        Flight flight = new Flight(
                resultSet.getLong("flight_id"),
                resultSet.getString("flight_no"),
                resultSet.getTimestamp("departure_date").toLocalDateTime(),
                resultSet.getString("departure_airport_code"),
                resultSet.getTimestamp("arrival_date").toLocalDateTime(),
                resultSet.getString("arrival_airport_code"),
                resultSet.getInt("aircraft_id"),
                resultSet.getString("status")
        );

        Ticket ticket = new Ticket();

        ticket.setId(resultSet.getLong("id"));
        ticket.setPassengerNo(resultSet.getString("passenger_no"));
        ticket.setPassengerName(resultSet.getString("passenger_name"));
//        ticket.setFlight(flight);
        ticket.setFlight(flightDao.findById(resultSet.getLong("flight_id"),resultSet.getStatement().getConnection()).orElse(null));
        ticket.setSeatNo(resultSet.getString("seat_no"));
        ticket.setCost(resultSet.getBigDecimal("cost"));
        return ticket;
    }

}
