/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.model.LoginRequest;
import com.ticketmaster.ticketplusclient.model.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 *
 * API login del Backend
 * 
 * Envia peticion POST al endpoint /api/auth/login con las credenciales de usuario en 
 * el formato de la clase LoginRequest. Si las credenciales son correctas se recibe
 * una respuesta en el formato de LoginResponse.
 * 
 * @param request Objeto que contiene las credenciales de usuario "username", "password"
 * 
 * @author Christian
 */
public interface AuthAPI {
    
    
    /**
     * Body: {"username": "...", "password": "..."}
     * Response 200: {"token": "...", "role": "..", "username": "..."}
     */
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    /**
     * El header es inyectado automaticamente por OkHttp: Authorization: Bearer <token>
     *Responde 204: sin cuerpo
     */
    @POST("api/auth/logout")
    Call<Void> logout();
    
}
