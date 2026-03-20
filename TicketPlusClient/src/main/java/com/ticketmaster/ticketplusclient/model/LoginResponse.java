/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 *
 * @author Erik
 */
public class LoginResponse {

    private String token;
    private String role;
    private String username;

    public LoginResponse() {
    }

    public LoginResponse(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
    public String getUsername(){
        return username;
    }
    
    @Override
    public String toString(){
        return "LoginResponse{username='"+username+"', role='"+role+"'}";
    }
}
