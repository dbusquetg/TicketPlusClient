/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.model.AgentRequest;
import com.ticketmaster.ticketplusclient.model.CommentDTO;
import com.ticketmaster.ticketplusclient.model.CreateCommentRequest;
import com.ticketmaster.ticketplusclient.model.CreateTicketRequest;
import com.ticketmaster.ticketplusclient.model.PriorityRequest;
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
 * Interfaz retrofit que define todos los endpoint de tickets y comentarios del 
 * backend.
 * 
 * <p>El token bearer es inyectado automaticamente por el interceptor de 
 * {@link ClientAPI} en cada peticion. El servidor usa el token para determinar 
 * el usuario y rol.</p>
 *
 * @author Christian
 * @see ClientAPI
 * @see TicketDTO
 * @see CommentDTO
 */
public interface TicketAPI {
    
    //Tickets
    
    /**
     * Lista de tickets - el servidor filtra por rol automaticamente
     * 
     * @return lista de {@link TicketDTO}
     */
    @GET("/api/tickets")
    Call<List<TicketDTO>> getTickets();
    
    /** Devuelve el detalle de un ticket por su id.
     * 
     * @param id identificador del ticket
     * @return {@link TicketDTO} con todos los campos del ticket
     */
    @GET("/api/tickets/{id}")
    Call<TicketDTO> getTicket(@Path("id") Long id);
    
    /** Crear nuevo ticket.
     * 
     * @param request datos del nuevo ticket.
     * @return {@link TicketDTO} creado con todos los campos
     */
    @POST("/api/tickets")
    Call<TicketDTO> createTicket(@Body CreateTicketRequest request);
    
    /** Cambiar estado de un ticket. Solo ADMIN
     * 
     * @param id identificador del ticket
     * @param request nuevo estado
     * @return {@link TicketDTO} actualizado con el agente asignado
     */
    @PATCH("/api/tickets/{id}/status")
    Call<TicketDTO> changeStatus(@Path("id") Long id, @Body StatusRequest request);
    
    /** Asigna el ticket al agente autenticado. Solo ADMIN.
     * 
     * @param id identificador del ticket
     * @return {@link TicketDTO} actualizado
     */
    @PATCH("/api/tickets/{id}/assign")
    Call<TicketDTO> assignToMe(@Path("id") Long id);
    
    /**
     * Asigna el ticket a un agente especifico por username. Solo ADMIN.
     * 
     * @param id identificador del ticket.
     * @param request username del agente
     * @return {@link TicketDTO} actualizado
     */
    @PATCH("api/tickets/{id}/agent")
    Call<TicketDTO> assignToAgent(@Path("id") Long id, @Body AgentRequest request);
    
    /**
     * Cambia la prioridad de un ticket.
     * 
     * @param id identificador del ticket
     * @param request nueva prioridad
     * @return {@link TicketDTO} actualizado
     */
    @PATCH("/api/tickets/{id}/priority")
    Call<TicketDTO> changePriority(@Path("id") Long id, @Body PriorityRequest request);
    
    /** Cerrar ticket (ADMIN y USER)
     * 
     * @param id identificador del ticket
     * @return {@link TicketDTO} con estado "Closed"
     */
    @PATCH("/api/tickets/{id}/close")
    Call<TicketDTO> closeTicket(@Path("id") Long id);
    
    //Comentarios
    
    /**
     * Obtiene el hilo de comentarios de un ticket ordenador por fecha.
     * 
     * @param id identificador del ticket
     * @return lista de {@link CommentDTO} del ticket
     */
    @GET("/api/tickets/{id}/comments")
    Call<List<CommentDTO>> getComments(@Path("id") Long id);
    
    /**
     * Añade un nuevo comentario a un ticket.
     * 
     * @param id identificador del ticket
     * @param request contenido del comentario
     * @return {@link CommentDTO} creado con todos los campos
     */
    @POST("/api/tickets/{id}/comments")
    Call<CommentDTO> addComment(@Path("id") Long id, @Body CreateCommentRequest request);
    
}
