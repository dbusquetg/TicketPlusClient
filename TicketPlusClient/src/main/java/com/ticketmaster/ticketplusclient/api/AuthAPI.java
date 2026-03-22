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
 * Interfaz Retrofit que define los endpoints de autenticación del backend TicketPlus.
 *
 * <p>Declara las operaciones HTTP de login y logout contra la API REST del servidor.
 * Retrofit genera automáticamente la implementación en tiempo de ejecución a partir
 * de las anotaciones de esta interfaz.</p>
 *
 * <p>El token Bearer es inyectado automáticamente en cada petición por el interceptor
 * configurado en {@link  ClientAPI}, por lo que el método {@code logout()} no
 * necesita declarar cabecera de autorización explícita.</p>
 * 
 * @author Christian
 * @see ClientAPI
 * @see LoginRequest
 * @see LoginResponse
 */
public interface AuthAPI {
    
    
    /**
     * Envía las credenciales del usuario al endpoint de login del servidor.
     *
     * <p>Realiza una petición HTTP POST a {@code /api/auth/login} con el cuerpo
     * serializado en JSON a partir del objeto {@link LoginRequest}.</p>
     *
     * @param request objeto {@link LoginRequest} con las credenciales del usuario
     * @return {@link Call} que encapsula la llamada asíncrona y la respuesta {@link LoginResponse}
     */
    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    /**
     * Notifica al servidor que el usuario ha cerrado sesión.
     *
     * <p>Realiza una petición HTTP POST a {@code api/auth/logout}. El token Bearer
     * de la sesión activa es añadido automáticamente en la cabecera
     * {@code Authorization} por el interceptor OkHttp de {@link ClientAPI}.</p>
     *
     * @return {@link Call} que encapsula la llamada asíncrona; el tipo de respuesta
     * es {@link Void} ya que el servidor no retorna cuerpo.
     */
    @POST("api/auth/logout")
    Call<Void> logout();
    
}
