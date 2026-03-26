/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la respuesta del servidor
 * ante una petición de login exitosa en TicketPlus.
 *
 * <p>Gson (a través de Retrofit) deserializa automáticamente el cuerpo JSON de la
 * respuesta HTTP 200 del endpoint {@code POST /api/auth/login} en una instancia
 * de esta clase.
 * </p>
 *
 * <p>El token devuelto es un JWT (JSON Web Token) que el cliente deberá incluir
 * como {@code Authorization: Bearer <token>} en todas las peticiones posteriores.
 * La gestión del token se centraliza en {@link com.ticketmaster.ticketplusclient.session.SessionManager}.</p>

 * @author Erik
 * @see com.ticketmaster.ticketplusclient.api.AuthAPI
 * @see com.ticketmaster.ticketplusclient.session.SessionManager
 */
public class LoginResponse {

    /** Token JWT de autenticación retornado por el servidor. */
    private String token;
    
    /** Rol del usuario autenticado (p.ej. {@code "ADMIN"} o {@code "USER"}). */
    private String role;
    
    /** Nombre de usuario autenticado. */
    private String username;

    /**
     * Constructor sin argumentos requerido por Gson para la deserialización.
     */
    public LoginResponse() {
    }

    /**
     * Crea una respuesta de login con todos los campos especificados.
     * Útil principalmente en pruebas unitarias.
     *
     * @param token token JWT de autenticación
     * @param role rol del usuario (p.ej. {@code "ADMIN"} o {@code "USER"})
     * @param username nombre del usuario autenticado
     */
    public LoginResponse(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }

    /**
     * Devuelve el token JWT de autenticación.
     *
     * @return token JWT
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token JWT de autenticación.
     *
     * @param token nuevo token JWT
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Devuelve el rol del usuario autenticado.
     *
     * @return rol del usuario (p.ej. {@code "ADMIN"} o {@code "USER"})
     */
    public String getRole() {
        return role;
    }

    /**
     * Establece el rol del usuario autenticado.
     *
     * @param role nuevo rol del usuario
     */
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Devuelve el nombre de usuario autenticado.
     *
     * @return nombre de usuario
     */
    public String getUsername(){
        return username;
    }
    
    /**
     * Devuelve una representación textual de la respuesta de login,
     * incluyendo el nombre de usuario y su rol (sin exponer el token).
     *
     * @return cadena con el formato {@code LoginResponse{username='...', role='...'}}
     */
    @Override
    public String toString(){
        return "LoginResponse{username='"+username+"', role='"+role+"'}";
    }
}
