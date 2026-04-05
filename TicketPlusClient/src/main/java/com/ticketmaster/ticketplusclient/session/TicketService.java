/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.api.TicketAPI;
import com.ticketmaster.ticketplusclient.model.StatusRequest;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import java.util.List;
import javax.swing.SwingUtilities;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author Christian G
 */
public class TicketService {
    
    private final TicketAPI ticketApi;
    
    public TicketService(){
        this.ticketApi = ClientAPI.createService(TicketAPI.class);
    }
    
    public void getTickets(TicketCallback<List<TicketDTO>> callback){
        ticketApi.getTickets().enqueue(new Callback<>(){
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
    
    public void changeStatus(Long id, String newStatus, TicketCallback<TicketDTO> callback){
        ticketApi.changeStatus(id, new StatusRequest(newStatus)).enqueue(new Callback<>(){
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
    
    public void assingToMe(Long id, TicketCallback<TicketDTO> callback){
        ticketApi.assignToMe(id).enqueue(new Callback<>() {
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
    
    public void closeTicket(Long id, TicketCallback<TicketDTO> callback){
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
    
    public interface TicketCallback<T>{
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
