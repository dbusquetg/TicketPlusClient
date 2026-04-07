/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO de peticion para asignar un agente al ticket.
 * 
 * <p> Solo lo usa el rol ADMIN para asginar el ticket a un agente diferente.</p>
 *
 * @author Christian G
 */
public class AgentRequest {
    
    /** Username del agente al que se asigna el ticket. */
    private String agentUsername;

    /**
     * Crea una peticion de asignacion de agente.
     * 
     * @param agentUsername  username del agente
     */
    public AgentRequest(String agentUsername) {
        this.agentUsername = agentUsername;
    }

    public String getAgentUsername() {
        return agentUsername;
    }

    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }
    
    
}
