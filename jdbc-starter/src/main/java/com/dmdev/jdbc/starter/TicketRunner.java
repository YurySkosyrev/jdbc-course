package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.dao.TicketDao;
import com.dmdev.jdbc.starter.dto.TicketFilter;
import com.dmdev.jdbc.starter.entity.Ticket;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class TicketRunner {

    public static void main(String[] args) {
        TicketFilter ticketFilter = new TicketFilter(3, 2, null, "A1");
        List<Ticket> tickets = TicketDao.getInstance().findALl(ticketFilter);
        System.out.println(tickets);
    }

    private static void updateTest() {
        TicketDao ticketDao = TicketDao.getInstance();
        Optional<Ticket> maybeTicket = ticketDao.findById(2L);
        System.out.println(maybeTicket);

        maybeTicket.ifPresent(ticket -> {
                ticket.setCost(BigDecimal.valueOf(666));
                ticketDao.update(ticket);
        });

        maybeTicket = ticketDao.findById(2L);
        System.out.println(maybeTicket);
    }

    private static void saveTest() {
        TicketDao ticketDao = TicketDao.getInstance();
        Ticket ticket = new Ticket();

        ticket.setPassengerNo("1234567");
        ticket.setPassengerName("Test");
        ticket.setFlightId(3L);
        ticket.setSeatNo("B3");
        ticket.setCost(BigDecimal.TEN);

        Ticket savedTicket = ticketDao.save(ticket);
        System.out.println(savedTicket);
    }
}
