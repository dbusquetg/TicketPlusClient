/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO de peticion para añadir comentario a un ticket.
 * 
 * <p> El servidor obtiene el autor del JWT.</p>
 *
 * @author Christian G
 */
public class CreateCommentRequest {
    
    /** Text del comentario a añadir. */
    private String content;
    
    /**
     * Crea una peticion de nuevo comentario.
     * 
     * @param content texto del comentario.
     */
    public CreateCommentRequest(String content){
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
}
