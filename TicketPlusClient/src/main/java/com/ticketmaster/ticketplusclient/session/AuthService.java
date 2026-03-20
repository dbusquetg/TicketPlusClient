/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.AuthAPI;
import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.model.LoginRequest;
import com.ticketmaster.ticketplusclient.model.LoginResponse;
import javax.swing.SwingUtilities;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *Clase para la gestion de la comunicacion entre la UX y el Cliente API.
 * 
 * @author Christian
 */
public class AuthService {
    
    private final AuthAPI authApi;
    
    public AuthService(){
        this.authApi = ClientAPI.createService(AuthAPI.class);
    }
    
    /**
     * Metodo que usa el ClientAPI para el envio de credenciales al servidor.
     * 
     * @param username
     * @param password
     * @param callback 
     */
    public void login(String username, String password, AuthCallback callback){
        LoginRequest request = new LoginRequest(username, password);
        
        authApi.login(request).enqueue(new Callback<LoginResponse>(){
            
            //En caso de respuesta del servidor.
            @Override
            public void onResponse(Call<LoginResponse> call,
                    Response<LoginResponse> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful() && response.body() != null){
                        LoginResponse body = response.body();

                        SessionManager.getInstance().startSession(
                                body.getToken(),
                                body.getRole(),
                                body.getUsername()
                        );
                        callback.onSuccess(body);
                    }else{
                        String message = switch(response.code()){
                            case 401 -> "Usuario o contraseña incorrectos";
                            case 403 -> "Tu cuenta esta desactivada";
                            case 500 -> "Error en el servidor";
                            default -> "Error: "+response.code();
                         };
                        callback.onError(message);
                     }
                });
            }
            //En caso de fallo en la conexion
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t){
                SwingUtilities.invokeLater(() ->
                        callback.onError("No ha sido posible conectar con el servidor "+t.getMessage())
                );
            }
        });
    }
    
    /**
     * Metodo para notificar al servidor cierre de session por medio del cliente API.
     * Gestiona el borrado local de los datos de la session.
     * 
     * @param onComplete 
     */
    public void logout(Runnable onComplete){
        authApi.logout().enqueue(new Callback<Void>() {
            
            @Override
            public void onResponse(Call<Void> call, Response<Void> response){
                SwingUtilities.invokeLater(() -> {
                    SessionManager.getInstance().clearSession();
                    ClientAPI.reset();
                    onComplete.run();
                });
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t){
                SwingUtilities.invokeLater(() -> {
                    SessionManager.getInstance().clearSession();
                    ClientAPI.reset();
                    onComplete.run();
                });
            }
        });
    }
    
    public interface AuthCallback{
        void onSuccess(LoginResponse response);
        void onError(String errorMessage);
    }
    
}

