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
 * Fábrica y configurador del cliente HTTP Retrofit para la comunicación con el backend TicketPlus.
 *
 * <p>Esta clase implementa el patrón <b>Singleton</b> sobre la instancia de {@link Retrofit}
 * y el patrón <b>Factory Method</b> a través de {@link #createService(Class)} para crear
 * implementaciones de las interfaces API declaradas (e.g., {@link AuthAPI}).</p>
 *
 * <p>Configura un interceptor OkHttp que añade automáticamente la cabecera
 * {@code Authorization: Bearer <token>} en todas las peticiones cuando el usuario
 * tiene una sesión activa en {@link SessionManager}, liberando así al resto de
 * componentes de gestionar la autenticación manualmente.</p>
 *
 * <p>La URL base del servidor se define en la constante {@link #BASE_URL}.</p>
 *
 * @author Christian
 */
public class ClientAPI {
    
    /**
     * URL base del backend
     */
    private static final String BASE_URL = System.getProperty("ticketplus.base.url", "http://10.2.99.25:8080/");
    
    
    /**
     * Instancia singleton de {@link Retrofit}. Se inicializa de forma diferida
     * (lazy) en la primera llamada a {@link #getClient()}.
     */
    private static Retrofit retrofit;
    
    /**
     * Devuelve la instancia singleton de {@link Retrofit} configurada con el
     * interceptor de autenticación y el conversor Gson.
     *
     * En caso de que la instancia no exista aún (primer acceso o tras llamar
     * a {@link #reset()}), crea un nuevo cliente OkHttp con los siguientes
     * interceptores:
     * <ul>
     *   <li><b>Interceptor de autenticación:</b> añade las cabeceras
     *       {@code Authorization}, {@code Accept} y {@code Content-Type} a cada
     *       petición. Si hay token en {@link SessionManager}, lo inyecta como
     *       Bearer token.</li>
     *   <li><b>HttpLoggingInterceptor:</b> registra el tráfico HTTP en consola
     *       para facilitar la depuración.</li>
     * </ul>
     * 
     *
     * @return la instancia singleton de {@link Retrofit} lista para ser usada
     */
    public static Retrofit getClient() {
        
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
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
    
    /**
     * Crea e instancia una implementación concreta de la interfaz API especificada.
     *
     * <p>Delega en {@link Retrofit#create(Class)} para generar dinámicamente
     * la implementación de la interfaz.</p>
     *
     * @param <T> tipo de la interfaz API a instanciar
     * @param serviceClass clase de la interfaz API (e.g., {@code AuthAPI.class})
     * @return implementación generada por Retrofit de la interfaz indicada
     */
    public static <T> T createService(Class<T> serviceClass){
        return getClient().create(serviceClass);
    }
    
    /**
     * Destruye la instancia singleton de {@link Retrofit}, forzando su
     * recreación en la siguiente llamada a {@link #getClient()}.
     *
     * <p>Debe invocarse tras un logout para asegurar que las peticiones
     * posteriores no reutilicen configuraciones de sesión antiguas
     * (especialmente el token de autenticación).</p>
     */
    public static void reset() {
        retrofit = null;
    }
    
}
