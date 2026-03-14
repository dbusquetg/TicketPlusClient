/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

/**
 *
 * @author
 */
// TODO: Crear formato de la request al endpoint Login
public class LoginRequest {

    //Crear variables que de los datos que se enviaran al endpoint de Login "user", "password"
    /*private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }*/
    private String nomusuari;
    private String contrasenya;

    public LoginRequest() {
    }

    public LoginRequest(String nomusuari, String contrasenya) {
        this.nomusuari = nomusuari;
        this.contrasenya = contrasenya;
    }

    public String getNomusuari() {
        return nomusuari;
    }

    public void setNomusuari(String nomusuari) {
        this.nomusuari = nomusuari;
    }

    public String getContrasenya() {
        return contrasenya;
    }

    public void setContrasenya(String contrasenya) {
        this.contrasenya = contrasenya;
    }
}
