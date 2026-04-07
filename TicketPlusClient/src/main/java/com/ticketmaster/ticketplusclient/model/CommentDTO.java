/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * DTO que representa un comentario dentro de un ticket.
 * 
 * <p> Gson traduce automaticamente las respuestas JSON del servidor en instancias
 * de esta clase. Se usa para mostrar el hilo de comentarios, como para representar
 * la respuesta tras añadir uno nuevo.</p>
 *
 * @author Christian G
 */
public class CommentDTO {
    
    /** Identificador unico del comentario. */
    private Long id;
    
    /** Referencia del ticket al que pertenece. */
    private String ticketRef;
    
    /** Titulo del ticket al que pertenece. */
    private String ticketTitle;
    
    /** Nombre del usuario que escribio el comentario. */
    private String author;
    
    /** Contenido del comentario. */
    private String content; 
    
    /** Fecha y hora de creacion. */
    private String createdAt;
    
    /** Requerido por GSON. */
    public CommentDTO(){}

    /**
     * Crea un comentario con todos los campos necesarios.
     * 
     * @param id identificador unico
     * @param ticketRef referencia del ticket
     * @param ticketTitle titulo del ticket
     * @param author autor del comentario
     * @param content contenido del comentario
     * @param createdAt fecha y hora de creacion
     */
    public CommentDTO(Long id, String ticketRef, String ticketTitle, String author, String content, String createdAt) {
        this.id = id;
        this.ticketRef = ticketRef;
        this.ticketTitle = ticketTitle;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketRef() {
        return ticketRef;
    }

    public void setTicketRef(String ticketRef) {
        this.ticketRef = ticketRef;
    }

    public String getTicketTitle() {
        return ticketTitle;
    }

    public void setTicketTitle(String ticketTitle) {
        this.ticketTitle = ticketTitle;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
}
