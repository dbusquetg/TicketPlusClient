/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 *
 * @author
 */
// TODO: Crear formato de respuesta del Endpoint Login
public class LoginResponse {

    //Crear variables de los tipos de respuesta del Endpoint
    /*private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }*/
    private boolean exit;
    private String missatge;

    public LoginResponse() {
    }

    public LoginResponse(boolean exit, String missatge) {
        this.exit = exit;
        this.missatge = missatge;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public String getMissatge() {
        return missatge;
    }

    public void setMissatge(String missatge) {
        this.missatge = missatge;
    }
}
