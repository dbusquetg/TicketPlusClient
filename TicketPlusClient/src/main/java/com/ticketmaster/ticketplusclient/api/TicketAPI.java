/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.model.TicketDTO;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 *
 * @author Christian
 */
public interface TicketAPI {
    
    //Lista de tickets - el servidor filtra por rol automaticamente
    @GET("/api/tickets")
    Call<List<TicketDTO>> getTickets();
    
    //Detalle de un ticket
    
}
