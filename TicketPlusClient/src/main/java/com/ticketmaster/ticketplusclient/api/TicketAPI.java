/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.model.StatusRequest;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 *
 * @author Christian
 */
public interface TicketAPI {
    
    //Lista de tickets - el servidor filtra por rol automaticamente
    @GET("/api/tickets")
    Call<List<TicketDTO>> getTickets();
    
    // Detalle de un ticket
    @GET("/api/tickets/{id}")
    Call<TicketDTO> getTicket(@Path("id") Long id);
    
    // Crear nuevo ticket (USER y ADMIN)
    @POST("/api/tickets")
    Call<TicketDTO> createTicket(@Body TicketDTO ticket);
    
    // Cambiar estado (ADMIN)
    @PATCH("/api/tickets/{id}/status")
    Call<TicketDTO> changeStatus(@Path("id") Long id, @Body StatusRequest request);
    
    // Asignarme el ticket (ADMIN)
    @PATCH("/api/tickets/{id}/assign")
    Call<TicketDTO> assignToMe(@Path("id") Long id);
    
    // Cerrar ticket (ADMIN y USER)
    @PATCH("/api/tickets/{id}/close")
    Call<TicketDTO> closeTicket(@Path("id") Long id);
    
}
