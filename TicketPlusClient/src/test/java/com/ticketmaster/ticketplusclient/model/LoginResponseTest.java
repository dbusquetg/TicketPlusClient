/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link LoginResponse}.
 *
 * <p>Verifica la correcta inicialización del objeto de respuesta de login,
 * el comportamiento de los getters/setters y que {@code toString()} no expone
 * el token de autenticación (seguridad).</p>
 *
 * @author Erik
 */
@DisplayName("LoginResponse - Pruebas unitarias del DTO")
class LoginResponseTest {

    @Test
    @DisplayName("Constructor completo inicializa todos los campos correctamente")
    void testConstructor_setsAllFields() {
        LoginResponse resp = new LoginResponse("tok123", "ADMIN", "christian");

        assertEquals("tok123", resp.getToken());
        assertEquals("ADMIN", resp.getRole());
        assertEquals("christian", resp.getUsername());
    }

    @Test
    @DisplayName("Constructor sin argumentos crea objeto con todos los campos null")
    void testDefaultConstructor_fieldsAreNull() {
        LoginResponse resp = new LoginResponse();

        assertNull(resp.getToken(),
                "El token debe ser null tras constructor vacío (para deserialización Gson)");
        assertNull(resp.getRole());
        assertNull(resp.getUsername());
    }

    @Test
    @DisplayName("setToken actualiza el campo token")
    void testSetToken_updatesField() {
        LoginResponse resp = new LoginResponse();
        resp.setToken("new-token");
        assertEquals("new-token", resp.getToken());
    }

    @Test
    @DisplayName("setRole actualiza el campo role")
    void testSetRole_updatesField() {
        LoginResponse resp = new LoginResponse();
        resp.setRole("USER");
        assertEquals("USER", resp.getRole());
    }

    @Test
    @DisplayName("toString() no expone el token de autenticación (seguridad)")
    void testToString_doesNotExposeToken() {
        LoginResponse resp = new LoginResponse("super-secret-token", "USER", "erik");
        String str = resp.toString();

        assertFalse(str.contains("super-secret-token"),
                "toString() NO debe incluir el token por seguridad");
        assertTrue(str.contains("erik"),
                "toString() debe incluir el username");
        assertTrue(str.contains("USER"),
                "toString() debe incluir el rol");
    }

    @Test
    @DisplayName("toString() tiene el formato LoginResponse{username='...', role='...'}")
    void testToString_hasExpectedFormat() {
        LoginResponse resp = new LoginResponse("tok", "ADMIN", "christian");
        String str = resp.toString();

        assertTrue(str.startsWith("LoginResponse{"),
                "toString() debe comenzar con 'LoginResponse{'");
        assertTrue(str.contains("username='christian'"));
        assertTrue(str.contains("role='ADMIN'"));
    }
}
