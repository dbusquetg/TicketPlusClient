/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO de peticion para cambiar la prioridad de un ticket.
 *
 * @author Christian G
 */
public class PriorityRequest {
    
    /** Nueva priodidad: HIGH, MEDIUM, LOW. */
    private String priority;

    public PriorityRequest(String priority) {
        this.priority = priority;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
    
}
