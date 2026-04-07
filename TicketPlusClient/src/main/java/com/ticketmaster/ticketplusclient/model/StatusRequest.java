/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO de peticion para cambiar el estado de un ticket.
 *
 * @author Christian G
 */
public class StatusRequest {
    
    /**
     * Nuevo estado del ticket.
     * Valores: Opened, In Progress, Pending, Solved, Closed.
     */
    private String status;
    
    /**
     * Crea peticion de cambio de estado
     * 
     * @param status  nuevo estado del ticket
     */
    public StatusRequest(String status) { 
        this.status = status; 
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() { 
        return status; 
    }
}
