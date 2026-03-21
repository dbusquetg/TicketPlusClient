/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.api;

import com.ticketmaster.ticketplusclient.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    
    private static Retrofit retrofit;
    
    /**
     * 
     */
    public static Retrofit getClient() {
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        
        if(retrofit == null){
            
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = SessionManager.getInstance().getToken();
                        
                        Request request;
                        
                        if(token != null){
                            request = original.newBuilder()
                                    .header("Authorization","Bearer " + token)
                                    .header("Accept", "application/json")
                                    .header("Content-Type", "application/json")
                                    .method(original.method(), original.body())
                                    .build();
                        }else{
                            request = original.newBuilder()
                                    .header("Accept","application/json")
                                    .header("Content-Type", "application/json")
                                    .method(original.method(), original.body())
                                    .build();
                        }
                        
                        return chain.proceed(request);
                            
                    })
                    
                    .addInterceptor(loggingInterceptor)
                    .build();
            
            retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        }
        return retrofit;
    }
    
    public static <T> T createService(Class<T> serviceClass){
        return getClient().create(serviceClass);
    }
    
    /**
     * Metodo para reiniciar el cliente Retrofit
     */
    public static void reset() {
        retrofit = null;
    }
    
}
