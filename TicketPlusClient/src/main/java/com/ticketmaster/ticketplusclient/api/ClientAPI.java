/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 *
 * @author Christian
 */
public class ClientAPI {
    
    private static final String BASE_URL = "http://10.2.99.25:8080/";
    
    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
    
    public static AuthAPI getAuthAPI(){
        return retrofit.create(AuthAPI.class);
    }
    
}
