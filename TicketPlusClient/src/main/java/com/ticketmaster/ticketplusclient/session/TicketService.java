/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.api.TicketAPI;
import com.ticketmaster.ticketplusclient.model.AgentRequest;
import com.ticketmaster.ticketplusclient.model.CommentDTO;
import com.ticketmaster.ticketplusclient.model.CreateCommentRequest;
import com.ticketmaster.ticketplusclient.model.CreateTicketRequest;
import com.ticketmaster.ticketplusclient.model.PriorityRequest;
import com.ticketmaster.ticketplusclient.model.StatusRequest;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import java.util.List;
import javax.swing.SwingUtilities;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Servicio que gestiona las operacions de tickets y comentarios.
 * 
 * <p>Actua como capa intermedia entre la GUI y {@link TicketAPI}.</p>
 *
 * @author Christian G
 * @see TicketAPI
 */
public class TicketService {
    
    /** Implementacion de la interfaz TicketAPI. */
    private final TicketAPI ticketApi;
    
    /**
     * Crea una nueva instancia de {@link TicketService}.
     */
    public TicketService(){
        this.ticketApi = ClientAPI.createService(TicketAPI.class);
    }
    
    //Tickets
    
    /**
     * Obtiene la lista de tickets segun el rol del usuario autenticado.
     * 
     * @param callback resultado de la operacion
     */
    public void getTickets(ServiceCallback<List<TicketDTO>> callback){
        ticketApi.getTickets().enqueue(new Callback<List<TicketDTO>>(){
           @Override
           public void onResponse(Call<List<TicketDTO>> call, Response<List<TicketDTO>> response){
               SwingUtilities.invokeLater(() -> {
                   if(response.isSuccessful() && response.body() != null)
                       callback.onSuccess(response.body());
                   else
                       callback.onError("Error al cargar tickets: "+response.code());
               });
           }
           @Override
           public void onFailure(Call<List<TicketDTO>> call, Throwable t){
               SwingUtilities.invokeLater(() ->
                    callback.onError("Sin conexino: "+t.getMessage()));
           }
        });
    }
    
    /**
     * Obtiene el detalle de un ticket por su identificador.
     * 
     * @param id identificador del ticket
     * @param callback  resultado de la operacion
     */
    public void getTicket(Long id, ServiceCallback<TicketDTO> callback){
        ticketApi.getTicket(id).enqueue(new Callback<TicketDTO>() {
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful() && response.body() != null)
                        callback.onSuccess(response.body());
                    else
                        callback.onError("Ticket no encontrado: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Crea un nuevo ticket con los datos proporcionados.
     * 
     * @param title titulo del ticket
     * @param description descripcion del ticket
     * @param priority prioridad del ticket (HIGH, MEDIUM, LOW)
     * @param callback resultado de la operacion
     */
    public void createTicket(String title, String description, String priority,
            ServiceCallback<TicketDTO> callback){
        CreateTicketRequest request = new CreateTicketRequest(title, description,
        priority);
        ticketApi.createTicket(request).enqueue(new Callback<TicketDTO>() {
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful() && response.body() != null)
                        callback.onSuccess(response.body());
                    else
                        callback.onError("Error al crear el ticket: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Cambia el estado de un ticket. Solo ADMIN.
     * 
     * @param id identificadir del ticket
     * @param newStatus nuevo estado
     * @param callback  resultado de la operacion
     */
    public void changeStatus(Long id, String newStatus, ServiceCallback<TicketDTO> callback){
        ticketApi.changeStatus(id, new StatusRequest(newStatus)).enqueue(new Callback<TicketDTO>(){
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error: "+ response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Asigna el ticket al agente autenticado. Solo ADMIN.
     * 
     * @param id identificador del ticket
     * @param callback resultado de la operacion
     */
    public void assignToMe(Long id, ServiceCallback<TicketDTO> callback){
        ticketApi.assignToMe(id).enqueue(new Callback<TicketDTO>() {
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Asigna el ticket a un agente por su username. Solo ADMIN.
     * 
     * @param id identificador del ticket
     * @param agentUsername username del agente
     * @param callback resultado de la operacion
     */
    public void assignToAgent(Long id, String agentUsername, ServiceCallback<TicketDTO> callback){
        ticketApi.assignToAgent(id, new AgentRequest(agentUsername)).enqueue(new Callback<TicketDTO>(){
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error al asignar agente: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Cambia la prioridad de un ticket.
     * 
     * @param id identificaro del ticket
     * @param newPriority nueva prioridad del ticket
     * @param callback resultado de la operacion
     */
    public void changePriority(Long id, String newPriority, ServiceCallback<TicketDTO> callback){
        ticketApi.changePriority(id, new PriorityRequest(newPriority)).enqueue(new Callback<TicketDTO>() {
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error al cambiar prioridad: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Cierra un ticket.
     * 
     * @param id identificador del ticket
     * @param callback  resultado de la operacion
     */
    public void closeTicket(Long id, ServiceCallback<TicketDTO> callback){
        ticketApi.closeTicket(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<TicketDTO> call, Response<TicketDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<TicketDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    //Comentarios
    
    /**
     * Obtiene un listado de comentarios de un ticket.
     * 
     * @param ticketId identifificador del ticket
     * @param callback resultado de la operacion
     */
    public void getComments(Long ticketId, ServiceCallback<List<CommentDTO>> callback){
        ticketApi.getComments(ticketId).enqueue(new Callback<List<CommentDTO>>() {
            @Override
            public void onResponse(Call<List<CommentDTO>> call, Response<List<CommentDTO>> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful() && response.body() != null)
                        callback.onSuccess(response.body());
                    else
                        callback.onError("Error al cargar comentarios: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<List<CommentDTO>> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    
    /**
     * Añade un comentario a un ticket.
     * 
     * @param ticketId identificador del ticket
     * @param content texto del comentario
     * @param callback resultado de la operacion
     */
    public void addComment(Long ticketId, String content, ServiceCallback<CommentDTO> callback){
        ticketApi.addComment(ticketId, new CreateCommentRequest(content)).enqueue(new Callback<CommentDTO>() {
            @Override
            public void onResponse(Call<CommentDTO> call, Response<CommentDTO> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful() && response.body() != null)
                        callback.onSuccess(response.body());
                    else
                        callback.onError("Error al añadir comentario: "+response.code());
                });
            }
            @Override
            public void onFailure(Call<CommentDTO> call, Throwable t){
                SwingUtilities.invokeLater(() -> callback.onError(t.getMessage()));
            }
        });
    }
    
    /**
     * Interfaz callback generica
     * 
     * @param <T> tipo del resultado en caso de exito
     */
    public interface ServiceCallback<T>{
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
