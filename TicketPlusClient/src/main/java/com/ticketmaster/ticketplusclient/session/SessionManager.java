/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

/**
 *
 * @author Erik
 */
public class SessionManager {
    
    private static SessionManager instance;
    
    private String token;
    private String role;
    private String username;
    
    private SessionManager() {}
    
    public static synchronized SessionManager getInstance(){
        if(instance == null){
            instance = new SessionManager();
        }
        return instance;
        
    }
    
    public void startSession(String token, String role, String username){
        this.token = token;
        this.role = role;
        this.username = username;
    }
    
    public void clearSession(){
        this.token = null;
        this.role = null;
        this.username = null;
    }
    
    public boolean isLoggedIn(){
        return token != null;
    }
    
    public boolean isAdmin(){
        return "ADMIN".equals(role);
    }
    
    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }
    
}
