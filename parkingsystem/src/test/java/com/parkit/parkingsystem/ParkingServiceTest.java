package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
//import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    //@BeforeEach
    private void setUp() throws Exception {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    @DisplayName("Process exiting vehicle from parking")
    public void processExitingVehicleTest() throws Exception {
      //GIVEN
      setUp();
      ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
      Ticket ticket = new Ticket();
      ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
      ticket.setParkingSpot(parkingSpot);
      ticket.setVehicleRegNumber("ABCDEF"); 

      //WHEN
      when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
      when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
      when(ticketDAO.getNbTicket(anyString())).thenReturn(0);

      parkingService.processExitingVehicle();

      //THEN
      verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
      verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
      assertTrue(ticket.getParkingSpot().isAvailable());
      assertEquals(ticketDAO.getTicket("ABCDEF").getPrice(), 1.5);
    }

    @Test
    @DisplayName("Process incoming vehicle from parking")
    public void testProcessIncomingVehicle() throws Exception {
      //GIVEN
      setUp();

      //WHEN
      when(inputReaderUtil.readSelection()).thenReturn(1);
      when(ticketDAO.getNbTicket(anyString())).thenReturn(0);   
      when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
      when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
      
      parkingService.processIncomingVehicle();
		  
      //THEN
      verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
      verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    @DisplayName("Process exiting vehicle and unable to udapte ticket")
    public void processExitingVehicleTestUnableUpdate() throws Exception {
      //GIVEN
      setUp();

      ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
      Ticket ticket = new Ticket();
      ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
      ticket.setParkingSpot(parkingSpot);
      ticket.setVehicleRegNumber("ABCDEF"); 
      
      //WHEN
      when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
		  when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		  parkingService.processExitingVehicle();

      //THEN
      assertEquals(0, parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class)));
      verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
      verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));
      assertFalse(ticket.getParkingSpot().isAvailable());
    }

    @Test
    @DisplayName("Get the next parking spot available")
    public void testGetNextParkingNumberIfAvailable() throws Exception {
      //GIVEN
      parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

      //WHEN
      when(inputReaderUtil.readSelection()).thenReturn(1);
      when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

      ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

      //THEN
      verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
      assertEquals(1, parkingSpot.getId());
      assertEquals(true, parkingSpot.isAvailable());
    }

    @Test
    @DisplayName("Get the next parking spot and the number is not found")
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {
      //GIVEN
      parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

      //WHEN
      when(inputReaderUtil.readSelection()).thenReturn(2);
      when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

      ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

      //THEN
      verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
      assertNull(parkingSpot);
    }


    @Test
    @DisplayName("Get the next parking spot and the type of vehicle is unknown")
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
      //GIVEN
      parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

      //WHEN
      when(inputReaderUtil.readSelection()).thenReturn(3);

      ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

      //THEN
      verify(parkingSpotDAO, times(0)).getNextAvailableSlot(any(ParkingType.class));
      assertNull(parkingSpot);
    }
}
