/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.model.LoginResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración con red para {@link AuthService}.
 *
 * <p>Estas pruebas se conectan al servidor backend REAL de TicketPlus
 * en http://10.2.99.25:8080/ para verificar el funcionamiento completo
 * del ciclo de autenticación.</p>
 *
 * <p><b>REQUISITO:</b> El servidor debe estar encendido y accesible
 * para que las pruebas pasen. Si el servidor está apagado, los tests
 * marcados con "FALLA CON SERVIDOR APAGADO" deben fallar — esto es
 * el comportamiento esperado y debe demostrarse en la videocaptura.</p>
 *
 * <p><b>Credenciales de prueba necesarias en el servidor:</b>
 * <ul>
 *   <li>Usuario ADMIN: username="admin", password="admin123"</li>
 *   <li>Usuario USER:  username="user",  password="user123"</li>
 * </ul>
 * Ajusta las constantes {@link #ADMIN_USER}, {@link #ADMIN_PASS},
 * {@link #USER_USER} y {@link #USER_PASS} a las credenciales reales
 * de tu servidor antes de ejecutar las pruebas.
 * </p>
 *
 * @author Christian
 */
@DisplayName("AuthService - Pruebas de integración con servidor REAL")
class AuthServiceIntegrationTest {

    // -------------------------------------------------------------------------
    // Credenciales de prueba — ajustar a las del servidor real
    // -------------------------------------------------------------------------

    /** Usuario con rol ADMIN existente en el servidor. */
    private static final String ADMIN_USER = "admin";
    /** Contraseña del usuario ADMIN. */
    private static final String ADMIN_PASS = "admin123";

    /** Usuario con rol USER existente en el servidor. */
    private static final String USER_USER = "user1";
    /** Contraseña del usuario USER. */
    private static final String USER_PASS = "admin123";

    /** Credenciales incorrectas (no deben existir en el servidor). */
    private static final String WRONG_USER = "usuario_inexistente_xyz";
    private static final String WRONG_PASS = "contrasena_incorrecta_xyz";

    /** Timeout en segundos para esperar respuestas del servidor real. */
    private static final int ASYNC_TIMEOUT_SECONDS = 10;

    // -------------------------------------------------------------------------
    // Instancia bajo prueba
    // -------------------------------------------------------------------------

    private AuthService authService;

    // =========================================================================
    // Setup / Teardown
    // =========================================================================

    /**
     * Crea una instancia real de {@link AuthService} (conectada al servidor
     * de producción via {@code ClientAPI}) y limpia la sesión local antes
     * de cada prueba para garantizar el aislamiento.
     */
    @BeforeEach
    void setUp() {
        authService = new AuthService();
        SessionManager.getInstance().clearSession();
    }

    /**
     * Limpia la sesión local y resetea ClientAPI después de cada prueba
     * para no contaminar las siguientes.
     */
    @AfterEach
    void tearDown() {
        SessionManager.getInstance().clearSession();
        com.ticketmaster.ticketplusclient.api.ClientAPI.reset();
    }

    // =========================================================================
    // PRUEBA a): Login correcto — credenciales válidas → acceso permitido
    // =========================================================================

    @Test
    @DisplayName("a) Login correcto (ADMIN): credenciales validas -> sesion iniciada [SERVIDOR ENCENDIDO]")
    void testLogin_withValidAdminCredentials_startsSession() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoginResponse> receivedResponse = new AtomicReference<>();
        AtomicReference<String> receivedError = new AtomicReference<>();

        // Act: login con credenciales reales del servidor
        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                receivedResponse.set(response);
                latch.countDown();
            }
            @Override
            public void onError(String errorMessage) {
                receivedError.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: el servidor no respondio en " + ASYNC_TIMEOUT_SECONDS + "s. "
                + "Comprueba que el servidor esta encendido en http://10.2.99.25:8080/");

        // Assert
        assertNull(receivedError.get(),
                "No debe haber error con credenciales validas. Error: " + receivedError.get());
        assertNotNull(receivedResponse.get(),
                "La respuesta del servidor no debe ser null");
        assertNotNull(receivedResponse.get().getToken(),
                "El servidor debe devolver un token JWT");
        assertNotNull(receivedResponse.get().getRole(),
                "El servidor debe devolver el rol del usuario");
        assertEquals(ADMIN_USER, receivedResponse.get().getUsername(),
                "El username devuelto debe coincidir con el enviado");

        assertTrue(SessionManager.getInstance().isLoggedIn(),
                "SessionManager debe reflejar sesion activa tras login correcto");
    }

    @Test
    @DisplayName("a) Login correcto (USER): credenciales validas -> sesion iniciada [SERVIDOR ENCENDIDO]")
    void testLogin_withValidUserCredentials_startsSession() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        AtomicReference<String> receivedError = new AtomicReference<>();

        authService.login(USER_USER, USER_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                success.set(true);
                latch.countDown();
            }
            @Override
            public void onError(String errorMessage) {
                receivedError.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: el servidor no respondio en " + ASYNC_TIMEOUT_SECONDS + "s");

        assertNull(receivedError.get(),
                "No debe haber error con credenciales USER validas. Error: " + receivedError.get());
        assertTrue(success.get(), "El callback onSuccess debe haberse invocado");
        assertTrue(SessionManager.getInstance().isLoggedIn());
    }

    // =========================================================================
    // PRUEBA b): Login incorrecto — credenciales inválidas → error controlado
    // =========================================================================

    @Test
    @DisplayName("b) Login incorrecto: credenciales invalidas -> error controlado [SERVIDOR ENCENDIDO]")
    void testLogin_withInvalidCredentials_returnsErrorMessage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();
        AtomicBoolean successCalled = new AtomicBoolean(false);

        authService.login(WRONG_USER, WRONG_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                successCalled.set(true);
                latch.countDown();
            }
            @Override
            public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: el servidor no respondio en " + ASYNC_TIMEOUT_SECONDS + "s");

        assertFalse(successCalled.get(),
                "onSuccess NO debe invocarse con credenciales incorrectas");
        assertNotNull(errorMsg.get(),
                "Debe existir un mensaje de error con credenciales incorrectas");
        assertFalse(errorMsg.get().isEmpty(),
                "El mensaje de error no debe estar vacio");
        assertFalse(SessionManager.getInstance().isLoggedIn(),
                "No debe haber sesion activa con credenciales incorrectas");

        System.out.println("Mensaje de error recibido del servidor: " + errorMsg.get());
    }

    // =========================================================================
    // PRUEBA c): Logout — cierre de sesión correcto
    // =========================================================================

    @Test
    @DisplayName("c) Logout correcto: sesion iniciada -> logout -> sesion local limpiada [SERVIDOR ENCENDIDO]")
    void testLogout_afterLogin_clearsLocalSession() throws InterruptedException {
        // Arrange: primero hacemos login para tener sesion activa
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse response) { loginLatch.countDown(); }
            @Override
            public void onError(String errorMessage) {
                loginError.set(errorMessage);
                loginLatch.countDown();
            }
        });

        assertTrue(loginLatch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout en el login previo al logout");
        assertNull(loginError.get(),
                "El login previo al logout fallo: " + loginError.get());
        assertTrue(SessionManager.getInstance().isLoggedIn(),
                "Debe haber sesion activa antes del logout");

        // Act: logout
        CountDownLatch logoutLatch = new CountDownLatch(1);
        authService.logout(logoutLatch::countDown);

        assertTrue(logoutLatch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: el logout no completo en " + ASYNC_TIMEOUT_SECONDS + "s");

        // Assert: sesion local limpiada
        assertFalse(SessionManager.getInstance().isLoggedIn(),
                "La sesion debe estar cerrada tras logout");
        assertNull(SessionManager.getInstance().getToken(),
                "El token debe ser null tras logout");
        assertNull(SessionManager.getInstance().getUsername(),
                "El username debe ser null tras logout");
    }

    // =========================================================================
    // PRUEBA d): Acceso a pantalla según tipo de usuario
    // =========================================================================

    @Test
    @DisplayName("d) Login ADMIN -> isAdmin() true -> DashboardAgentGUI [SERVIDOR ENCENDIDO]")
    void testLogin_adminUser_sessionReflectsAdminRole() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override
            public void onError(String e) { loginError.set(e); latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull(loginError.get(), "Login ADMIN fallo: " + loginError.get());

        assertTrue(SessionManager.getInstance().isAdmin(),
                "isAdmin() debe ser true para usuario ADMIN -> LoginGUI abre DashboardAgentGUI");
    }

    @Test
    @DisplayName("d) Login USER -> isAdmin() false -> DashboardUserGUI [SERVIDOR ENCENDIDO]")
    void testLogin_normalUser_sessionReflectsUserRole() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(USER_USER, USER_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override
            public void onError(String e) { loginError.set(e); latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull(loginError.get(), "Login USER fallo: " + loginError.get());

        assertFalse(SessionManager.getInstance().isAdmin(),
                "isAdmin() debe ser false para usuario USER -> LoginGUI abre DashboardUserGUI");
    }

    // =========================================================================
    // PRUEBA e): Token almacenado en memoria tras login
    // =========================================================================

    @Test
    @DisplayName("e) Tras login correcto el token JWT queda almacenado en SessionManager [SERVIDOR ENCENDIDO]")
    void testLogin_tokenStoredInSessionManager() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override
            public void onError(String e) { loginError.set(e); latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull(loginError.get(), "Login fallo: " + loginError.get());

        String token = SessionManager.getInstance().getToken();
        assertNotNull(token, "El token debe estar almacenado en memoria tras login");
        assertFalse(token.isEmpty(), "El token no debe ser una cadena vacia");

        System.out.println("Token recibido (primeros 20 chars): "
                + token.substring(0, Math.min(20, token.length())) + "...");
    }

    // =========================================================================
    // PRUEBA ESPECIAL: SERVIDOR APAGADO — debe fallar
    // =========================================================================

    /**
     * PRUEBA DE FALLO CON SERVIDOR APAGADO.
     *
     * Esta prueba demuestra que cuando el servidor esta apagado o no es
     * accesible, el cliente recibe el error "No ha sido posible conectar"
     * en lugar de bloquearse indefinidamente.
     *
     * INSTRUCCIONES PARA LA VIDEOCAPTURA:
     *   1. Apaga el servidor (detén el proceso del backend).
     *   2. Ejecuta SOLO este test: mvn test -Dtest=AuthServiceIntegrationTest#testLogin_whenServerIsDown_clientReceivesConnectionError
     *   3. Graba en video como el test FALLA con el mensaje de conexion.
     *   4. Vuelve a encender el servidor y ejecuta todos los tests para
     *      demostrar que ahora pasan.
     *
     * NOTA: Con el servidor encendido, este test TAMBIEN debe PASAR porque
     * el servidor NO devuelve el mensaje de "No ha sido posible conectar"
     * (sino un HTTP correcto), por lo que la assertion fallara intencionadamente.
     * Ejecuta este test SOLO con el servidor apagado.
     */
    @Test
    @DisplayName("SERVIDOR APAGADO: login falla con mensaje de conexion [EJECUTAR CON SERVIDOR APAGADO]")
    void testLogin_whenServerIsDown_clientReceivesConnectionError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();
        AtomicBoolean successCalled = new AtomicBoolean(false);

        // Act: intentar login con el servidor apagado
        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                successCalled.set(true);
                latch.countDown();
            }
            @Override
            public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        // Timeout generoso para dar margen al timeout de red de OkHttp
        assertTrue(latch.await(15, TimeUnit.SECONDS),
                "El cliente quedo bloqueado indefinidamente — onError nunca se invoco");

        // Assert: el cliente recibio un error de conexion, no un error HTTP
        assertFalse(successCalled.get(),
                "onSuccess NO debe invocarse si el servidor esta apagado");
        assertNotNull(errorMsg.get(),
                "Debe existir mensaje de error cuando el servidor esta apagado");
        assertTrue(
                errorMsg.get().startsWith("No ha sido posible conectar"),
                "El mensaje debe indicar fallo de conexion. Mensaje recibido: " + errorMsg.get()
        );
        assertFalse(SessionManager.getInstance().isLoggedIn(),
                "No debe haber sesion activa si el servidor esta apagado");
    }
}