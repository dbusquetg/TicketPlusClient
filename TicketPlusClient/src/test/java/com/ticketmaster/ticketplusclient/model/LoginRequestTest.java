/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link LoginRequest}.
 *
 * <p>Verifica que el constructor y los métodos getter/setter del objeto
 * de transferencia de datos funcionan correctamente antes de ser serializado
 * a JSON por Retrofit.</p>
 *
 * @author Erik
 */
@DisplayName("LoginRequest - Pruebas unitarias del DTO")
class LoginRequestTest {

    @Test
    @DisplayName("Constructor inicializa username y password correctamente")
    void testConstructor_setsFieldsCorrectly() {
        LoginRequest req = new LoginRequest("christian", "pass123");

        assertEquals("christian", req.getUsername(),
                "El username no coincide con el pasado al constructor");
        assertEquals("pass123", req.getPassword(),
                "El password no coincide con el pasado al constructor");
    }

    @Test
    @DisplayName("setUsername actualiza el campo username")
    void testSetUsername_updatesField() {
        LoginRequest req = new LoginRequest("original", "pass");
        req.setUsername("updated");
        assertEquals("updated", req.getUsername());
    }

    @Test
    @DisplayName("setPassword actualiza el campo password")
    void testSetPassword_updatesField() {
        LoginRequest req = new LoginRequest("user", "original");
        req.setPassword("newpass");
        assertEquals("newpass", req.getPassword());
    }

    @Test
    @DisplayName("Constructor permite username y password vacíos (validación en GUI)")
    void testConstructor_allowsEmptyStrings() {
        LoginRequest req = new LoginRequest("", "");
        assertEquals("", req.getUsername());
        assertEquals("", req.getPassword());
    }

    @Test
    @DisplayName("Constructor permite valores null (sin validación en DTO)")
    void testConstructor_allowsNullValues() {
        LoginRequest req = new LoginRequest(null, null);
        assertNull(req.getUsername());
        assertNull(req.getPassword());
    }
}