/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 *
 * 
 * @author Christian
 */
public class TicketDTO {
    
    private Long id;
    private String ref;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String createdBy;
    private String agent;
    private String createdAt;
    private String resolvedAt;
    
    public TicketDTO(){
        
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
    
    public String getCreatedAt(){
        return createdAt;
    }
    
    public void setCreatedAt(String c){
        this.createdAt = c;
    }
    
    public String getResolvedAt() {
        return resolvedAt;
    }   

    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    
    
}
