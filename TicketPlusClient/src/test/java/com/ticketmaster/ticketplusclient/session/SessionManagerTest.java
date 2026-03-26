/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SessionManager}.
 *
 * <p>Verifica el ciclo de vida completo de la sesión: estado inicial sin sesión,
 * inicio de sesión, consulta de datos, verificación de rol y cierre de sesión.
 * También valida el comportamiento del patrón Singleton.</p>
 *
 * @author Erik
 */
@DisplayName("SessionManager - Pruebas unitarias")
class SessionManagerTest {

    private SessionManager session;

    /**
     * Obtiene la instancia de SessionManager y la resetea antes de cada prueba
     * para garantizar el aislamiento entre tests.
     */
    @BeforeEach
    void setUp() {
        session = SessionManager.getInstance();
        session.clearSession();
    }

    // -------------------------------------------------------------------------
    // Estado inicial
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sin sesión activa al iniciar en estado limpio")
    void testNoActiveSessionInitially() {
        assertFalse(session.isLoggedIn(),
                "isLoggedIn() debe ser false cuando no hay sesión activa");
        assertNull(session.getToken(),
                "getToken() debe ser null cuando no hay sesión activa");
        assertNull(session.getRole(),
                "getRole() debe ser null cuando no hay sesión activa");
        assertNull(session.getUsername(),
                "getUsername() debe ser null cuando no hay sesión activa");
    }

    // -------------------------------------------------------------------------
    // startSession
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("startSession almacena token, rol y username correctamente")
    void testStartSession_storesDataCorrectly() {
        session.startSession("jwt-test-token", "ADMIN", "christian");

        assertTrue(session.isLoggedIn(),
                "isLoggedIn() debe ser true tras startSession");
        assertEquals("jwt-test-token", session.getToken(),
                "El token almacenado no coincide con el esperado");
        assertEquals("ADMIN", session.getRole(),
                "El rol almacenado no coincide con el esperado");
        assertEquals("christian", session.getUsername(),
                "El username almacenado no coincide con el esperado");
    }

    @Test
    @DisplayName("startSession con rol USER guarda datos correctamente")
    void testStartSession_userRole_storesDataCorrectly() {
        session.startSession("tok-user-456", "USER", "erik");

        assertTrue(session.isLoggedIn());
        assertEquals("tok-user-456", session.getToken());
        assertEquals("USER", session.getRole());
        assertEquals("erik", session.getUsername());
    }

    // -------------------------------------------------------------------------
    // isAdmin
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isAdmin() devuelve true cuando el rol es ADMIN")
    void testIsAdmin_returnsTrueForAdminRole() {
        session.startSession("token", "ADMIN", "christian");
        assertTrue(session.isAdmin(),
                "isAdmin() debe ser true para rol ADMIN");
    }

    @Test
    @DisplayName("isAdmin() devuelve false cuando el rol es USER")
    void testIsAdmin_returnsFalseForUserRole() {
        session.startSession("token", "USER", "erik");
        assertFalse(session.isAdmin(),
                "isAdmin() debe ser false para rol USER");
    }

    @Test
    @DisplayName("isAdmin() devuelve false cuando no hay sesión activa")
    void testIsAdmin_returnsFalseWithNoSession() {
        assertFalse(session.isAdmin(),
                "isAdmin() debe ser false cuando no hay sesión");
    }

    // -------------------------------------------------------------------------
    // clearSession
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("clearSession elimina todos los datos de sesión")
    void testClearSession_removesAllData() {
        session.startSession("jwt-test-token", "USER", "erik");
        session.clearSession();

        assertFalse(session.isLoggedIn(),
                "isLoggedIn() debe ser false tras clearSession");
        assertNull(session.getToken(),
                "getToken() debe ser null tras clearSession");
        assertNull(session.getRole(),
                "getRole() debe ser null tras clearSession");
        assertNull(session.getUsername(),
                "getUsername() debe ser null tras clearSession");
    }

    @Test
    @DisplayName("clearSession en sesión ya vacía no lanza excepción")
    void testClearSession_onAlreadyClearedSession_doesNotThrow() {
        assertDoesNotThrow(() -> session.clearSession(),
                "clearSession() en sesión vacía no debe lanzar excepción");
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getInstance() devuelve siempre la misma instancia (Singleton)")
    void testSingleton_sameInstanceReturned() {
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();
        assertSame(instance1, instance2,
                "getInstance() debe devolver siempre la misma instancia");
    }

    @Test
    @DisplayName("Los datos de sesión son compartidos entre instancias del Singleton")
    void testSingleton_dataSharedAcrossInstances() {
        SessionManager.getInstance().startSession("shared-token", "ADMIN", "christian");
        assertEquals("shared-token", SessionManager.getInstance().getToken(),
                "El token debe ser accesible desde cualquier llamada a getInstance()");
    }
}