/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 *
 * Cliente API Rest del Backend
 * 
 * Esta clase configura retrofit para dar acceso a las interfaces API creadas
 * 
 * Usa JacsonConverterFactory para trasnformar las peticiones y respuestas en
 * JSON
 * 
 * @author Christian
 */
public class ClientAPI {
    
    /**
     * URL base del backend
     */
    private static final String BASE_URL = "http://10.2.99.25:8080/";
    
    /**
     * Instancia de Retrofit
     */
    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
    
    /**
     * 
     * Genera una instancia de la API de login
     * 
     * @return implementacion de la interfaz AuthAPI para generar llamadas HTTP
     */
    public static AuthAPI getAuthAPI(){
        return retrofit.create(AuthAPI.class);
    }
    
}
