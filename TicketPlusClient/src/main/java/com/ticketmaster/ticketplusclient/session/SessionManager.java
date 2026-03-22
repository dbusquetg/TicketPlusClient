/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

/**
 * Gestor de sesión de usuario para la aplicación de escritorio TicketPlus.
 *
 * <p>Implementa el patrón <b>Singleton</b> con sincronización para garantizar
 * que exista únicamente una instancia del gestor de sesión durante toda la
 * ejecución de la aplicación. Almacena en memoria el token de autenticación,
 * el rol y el nombre de usuario correspondientes a la sesión activa.</p>
 *
 * <p>El token se mantiene exclusivamente en memoria (nunca se persiste en disco
 * ni en base de datos local), de acuerdo con los requisitos de seguridad del
 * proyecto. Al hacer logout, todos los datos de sesión son eliminados mediante
 * {@link #clearSession()}.</p>
 *
 * <p>El token es recuperado por {@link com.ticketmaster.ticketplusclient.api.ClientAPI}
 * en cada petición HTTP para inyectarlo como cabecera {@code Authorization: Bearer}.</p>
 *
 * @author Erik
 * @see com.ticketmaster.ticketplusclient.api.ClientAPI
 * @see AuthService
 */
public class SessionManager {
    
    /** Única instancia del gestor de sesión (patrón Singleton). */
    private static SessionManager instance;
    
    /** Token JWT de autenticación de la sesión activa. {@code null} si no hay sesión. */
    private String token;
    
    /** Rol del usuario autenticado (p.ej. {@code "ADMIN"} o {@code "USER"}). */
    private String role;
    
    /** Nombre de usuario de la sesión activa. */
    private String username;
    
    /**
     * Constructor privado para prevenir la instanciación directa desde el exterior,
     * siguiendo el patrón Singleton.
     */
    private SessionManager() {}
    
    /**
     * Devuelve la instancia única de {@link SessionManager}, creándola si aún
     * no existe. El método está sincronizado para ser seguro en entornos
     * multihilo.
     *
     * @return la instancia singleton de {@link SessionManager}
     */
    public static synchronized SessionManager getInstance(){
        if(instance == null){
            instance = new SessionManager();
        }
        return instance;
        
    }
    
    /**
     * Inicia una nueva sesión almacenando en memoria los datos del usuario autenticado.
     *
     * <p>Debe invocarse tras recibir una respuesta de login exitosa del servidor.</p>
     *
     * @param token token JWT de autenticación recibido del servidor
     * @param role rol del usuario (p.ej. {@code "ADMIN"} o {@code "USER"})
     * @param username nombre de usuario autenticado
     */
    public void startSession(String token, String role, String username){
        this.token = token;
        this.role = role;
        this.username = username;
    }
    
    /**
     * Cierra la sesión activa eliminando de memoria todos los datos de sesión.
     *
     * <p>Tras esta llamada, {@link #isLoggedIn()} retornará {@code false} y
     * {@link #getToken()} retornará {@code null}. Debe invocarse siempre
     * durante el proceso de logout.</p>
     */
    public void clearSession(){
        this.token = null;
        this.role = null;
        this.username = null;
    }
    
    /**
     * Indica si existe una sesión de usuario activa.
     *
     * @return {@code true} si hay un token de sesión válido en memoria;
     * {@code false} en caso contrario
     */
    public boolean isLoggedIn(){
        return token != null;
    }
    
    /**
     * Indica si el usuario de la sesión activa tiene rol de administrador.
     *
     * @return {@code true} si el rol es {@code "ADMIN"}; {@code false} en
     * cualquier otro caso o si no hay sesión activa
     */
    public boolean isAdmin(){
        return "ADMIN".equals(role);
    }
    
    /**
     * Devuelve el token JWT de autenticación de la sesión activa.
     *
     * @return el token JWT, o {@code null} si no hay sesión activa
     */
    public String getToken() {
        return token;
    }

    /**
     * Devuelve el rol del usuario de la sesión activa.
     *
     * @return el rol (p.ej. {@code "ADMIN"} o {@code "USER"}), o {@code null}
     * si no hay sesión activa
     */
    public String getRole() {
        return role;
    }

    /**
     * Devuelve el nombre de usuario de la sesión activa.
     *
     * @return el nombre de usuario, o {@code null} si no hay sesión activa
     */
    public String getUsername() {
        return username;
    }
    
}
