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
 * Servicio de autenticación que actúa como capa intermediaria entre la interfaz
 * gráfica (GUI) y el cliente HTTP Retrofit ({@link ClientAPI}).
 *
 * <p>Gestiona de forma asíncrona las operaciones de login y logout, garantizando
 * que los callbacks que actualizan la UI se ejecuten siempre en el
 * Event Dispatch Thread (EDT) de Swing mediante {@link SwingUtilities#invokeLater(Runnable)}.</p>
 *
 * <p>Tras un login exitoso, almacena los datos de sesión en {@link SessionManager}.
 * Tras un logout (exitoso o fallido), limpia la sesión local y reinicia el
 * cliente Retrofit mediante {@link ClientAPI#reset()} para eliminar el token
 * del interceptor de cabeceras.</p>
 *
 * @author Christian
 * @see AuthAPI
 * @see SessionManager
 * @see ClientAPI
 */
public class AuthService {
    
    /** Instancia de la interfaz API de autenticación generada por Retrofit. */
    private final AuthAPI authApi;
    
    /**
     * Crea una nueva instancia de {@link AuthService} inicializando la interfaz
     * {@link AuthAPI} a través de {@link ClientAPI#createService(Class)}.
     */
    public AuthService(){
        this.authApi = ClientAPI.createService(AuthAPI.class);
    }
    
    AuthService(AuthAPI authApi) {
        this.authApi = authApi;
    }
    
    /**
     * Envía las credenciales de usuario al servidor de forma asíncrona y
     * notifica el resultado mediante el callback proporcionado.
     *
     * <p>Si el servidor responde con HTTP 200 y un cuerpo válido, inicia la
     * sesión en {@link SessionManager} e invoca {@link AuthCallback#onSuccess(LoginResponse)}.
     * En caso de respuesta de error o fallo de conexión, invoca
     * {@link AuthCallback#onError(String)} con un mensaje descriptivo.</p>
     *
     * Códigos de error HTTP gestionados:
     * <ul>
     *   <li>401: credenciales incorrectas</li>
     *   <li>403: cuenta desactivada</li>
     *   <li>500: error interno del servidor</li>
     * </ul>
     * 
     *
     * @param username nombre de usuario a autenticar
     * @param password contraseña del usuario
     * @param callback interfaz {@link AuthCallback} que recibirá el resultado
     * de la operación en el EDT de Swing
     */
    public void login(String username, String password, AuthCallback callback){
        LoginRequest request = new LoginRequest(username, password);
        
        authApi.login(request).enqueue(new Callback<LoginResponse>(){
            
            /**
             * Invocado cuando el servidor responde (con o sin éxito HTTP).
             *
             * @param call llamada Retrofit original
             * @param response respuesta HTTP recibida del servidor
             */
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
            
            /**
             * Invocado cuando se produce un fallo de red o de conexión con el servidor.
             *
             * @param call llamada Retrofit original
             * @param t excepción que describe el motivo del fallo
             */
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t){
                SwingUtilities.invokeLater(() ->
                        callback.onError("No ha sido posible conectar con el servidor "+t.getMessage())
                );
            }
        });
    }
    
    /**
     * Notifica al servidor el cierre de sesión de forma asíncrona y, al completar
     * (independientemente del resultado), limpia la sesión local y ejecuta
     * el callback de finalización.
     *
     * <p>El logout local (limpieza de {@link SessionManager} y reinicio de
     * {@link ClientAPI}) se realiza siempre, incluso si el servidor no responde,
     * garantizando que el cliente quede en un estado sin sesión.</p>
     *
     * @param onComplete {@link Runnable} que se ejecutará en el EDT de Swing
     * una vez completado el proceso de logout (éxito o fallo)
     */
    public void logout(Runnable onComplete){
        authApi.logout().enqueue(new Callback<Void>() {
            
            /**
             * Invocado cuando el servidor responde al logout.
             *
             * @param call llamada Retrofit original
             * @param response respuesta HTTP recibida del servidor
             */
            @Override
            public void onResponse(Call<Void> call, Response<Void> response){
                SwingUtilities.invokeLater(() -> {
                    if(response.isSuccessful()){
                        System.out.println("Logout OK — servidor respondió: " + response.code());
                    } else {
                        System.err.println("Logout FALLIDO — servidor respondió: " + response.code());
                    }
                    SessionManager.getInstance().clearSession();
                    ClientAPI.reset();
                    onComplete.run();
                });
            }
            
            /**
             * Invocado cuando hay un fallo de red durante el logout. Aun así
             * se limpia la sesión local para garantizar la seguridad del cliente.
             *
             * @param call llamada Retrofit original
             * @param t excepción que describe el motivo del fallo
             */
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
    
    /**
     * Interfaz de callback para notificar el resultado de las operaciones de
     * autenticación asíncronas al componente llamante (normalmente la GUI).
     *
     * <p>Los métodos de esta interfaz son siempre invocados en el EDT de Swing,
     * por lo que es seguro actualizar componentes gráficos desde su implementación.</p>
     */
    public interface AuthCallback{
        
        /**
         * Invocado cuando el login se ha completado correctamente.
         *
         * @param response objeto {@link LoginResponse} con el token, rol y
         * nombre de usuario recibidos del servidor
         */
        void onSuccess(LoginResponse response);
        
        /**
         * Invocado cuando el login ha fallado (credenciales incorrectas,
         * cuenta desactivada, error del servidor o fallo de red).
         *
         * @param errorMessage mensaje descriptivo del error, listo para
         * mostrarse al usuario
         */
        void onError(String errorMessage);
    }
    
}

