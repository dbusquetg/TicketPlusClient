/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la petición de login
 * enviada al servidor TicketPlus.
 *
 * <p>Esta clase es serializada automáticamente a JSON por Gson (a través de
 * Retrofit) antes de ser enviada al endpoint {@code POST /api/auth/login}.
 * </p>
 *
 * <p>La contraseña se envía en texto plano en el cuerpo de la petición HTTP.
 * El cifrado de la comunicación depende del protocolo de transporte utilizado.</p>
 *
 * @author Erik
 * @see com.ticketmaster.ticketplusclient.api.AuthAPI
 */
public class LoginRequest {

    /** Nombre de usuario para la autenticación. */
    private String username;
    
    /** Contraseña del usuario para la autenticación. */
    private String password;

    /**
     * Crea una nueva petición de login con las credenciales proporcionadas.
     *
     * @param username nombre de usuario a autenticar
     * @param password contraseña del usuario
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Devuelve el nombre de usuario de la petición.
     *
     * @return nombre de usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * Establece el nombre de usuario de la petición.
     *
     * @param username nuevo nombre de usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Devuelve la contraseña de la petición.
     *
     * @return contraseña del usuario
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establece la contraseña de la petición.
     *
     * @param password nueva contraseña
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
