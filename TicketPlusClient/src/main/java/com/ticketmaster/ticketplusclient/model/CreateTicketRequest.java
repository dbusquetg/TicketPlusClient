/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO de peticion para la creacion de un nuevo ticket.
 * 
 * <p> Contiene los datos que el cliente envia al servidor, el resto los crea
 * el servidor automaticamente (ref, status, createdBy, createdAt).</p>
 * 
 * 
 * @author Christian G
 */
public class CreateTicketRequest {
    
    /** Titulo descriptivo del ticket. */
    private String title;
    
    /** Descripcion detallada del ticket*/
    private String description;
    
    /** Nivel de prioridad (HIGH, MEDIUM, LOW)*/
    private String priority;
    
    /**
     * Crea una nueva peticion de creacion de ticket.
     * 
     * @param title titulo del ticket
     * @param description descripcion del ticket
     * @param priority prioridad (HIGH, MEDIUM, LOW)
     */
    public CreateTicketRequest(String title, String description, String priority){
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    } 
    
}
