package com.dmdev.jdbc.starter;

import com.dmdev.jdbc.starter.dao.TicketDao;
import com.dmdev.jdbc.starter.entity.Ticket;

import java.math.BigDecimal;

public class TicketRunner {

    public static void main(String[] args) {

        TicketDao ticketDao = TicketDao.getInstance();
        boolean deleteResult = ticketDao.delete(59L);
        System.out.println(deleteResult);

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
