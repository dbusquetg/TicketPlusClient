/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.AuthAPI;
import com.ticketmaster.ticketplusclient.model.LoginResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración con red para {@link AuthService}.
 *
 * <p>Utiliza {@link MockWebServer} (OkHttp) para simular las respuestas del servidor
 * backend sin necesidad de conexión real. La inyección de la URL del MockWebServer
 * se realiza a través del constructor package-private de {@link AuthService},
 * evitando reflexión y garantizando el aislamiento total entre tests.</p>
 *
 * <p><b>Requisito:</b> {@code AuthService} debe exponer el constructor package-private
 * {@code AuthService(AuthAPI authApi)} para uso exclusivo en tests.</p>
 *
 * <p><b>Dependencias en pom.xml:</b>
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.squareup.okhttp3&lt;/groupId&gt;
 *   &lt;artifactId&gt;mockwebserver&lt;/artifactId&gt;
 *   &lt;version&gt;4.12.0&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * &lt;dependency&gt;
 *   &lt;groupId&gt;org.junit.jupiter&lt;/groupId&gt;
 *   &lt;artifactId&gt;junit-jupiter&lt;/artifactId&gt;
 *   &lt;version&gt;5.10.2&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 * </p>
 *
 * @author Christian
 */
@DisplayName("AuthService - Pruebas de integración con red (MockWebServer)")
class AuthServiceIntegrationTest {

    private MockWebServer mockWebServer;
    private AuthService authService;

    private static final int ASYNC_TIMEOUT_SECONDS = 5;

    // =========================================================================
    // Setup / Teardown
    // =========================================================================

    /**
     * Inicia un MockWebServer nuevo, construye un Retrofit apuntando a él
     * (replicando el interceptor de autenticación Bearer de ClientAPI) y
     * crea el AuthService con la AuthAPI generada a partir de ese Retrofit.
     * De esta forma, cada test tiene su propio servidor simulado aislado.
     *
     * @throws IOException si el MockWebServer no puede iniciarse
     */
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Replicate the auth interceptor from ClientAPI to test headers correctly
        OkHttpClient testHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = SessionManager.getInstance().getToken();
                    Request request;
                    if (token != null) {
                        request = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();
                    } else {
                        request = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();
                    }
                    return chain.proceed(request);
                })
                .build();

        Retrofit testRetrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(testHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Inject via package-private constructor — clean, no reflection needed
        AuthAPI testAuthApi = testRetrofit.create(AuthAPI.class);
        authService = new AuthService(testAuthApi);

        SessionManager.getInstance().clearSession();
    }

    /**
     * Detiene el MockWebServer y limpia el estado de sesión y Retrofit
     * para no contaminar otras pruebas.
     *
     * @throws IOException si el MockWebServer no puede detenerse
     */
    @AfterEach
    void tearDown() throws IOException {
        try {
            mockWebServer.shutdown();
        } catch (IOException ignored) {
            // Already shut down in testLogin_whenServerIsDown — safe to ignore
        }
        SessionManager.getInstance().clearSession();
    }

    // =========================================================================
    // PRUEBA a): Login correcto — credenciales válidas → acceso permitido
    // =========================================================================

    @Test
    @DisplayName("a) Login correcto: HTTP 200 -> sesion iniciada en SessionManager")
    void testLogin_withValidCredentials_startsSession() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"jwt-abc123\",\"role\":\"ADMIN\",\"username\":\"christian\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoginResponse> receivedResponse = new AtomicReference<>();
        AtomicReference<String> receivedError = new AtomicReference<>();

        authService.login("christian", "pass123", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) {
                receivedResponse.set(response);
                latch.countDown();
            }
            @Override public void onError(String errorMessage) {
                receivedError.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: no se recibio respuesta en " + ASYNC_TIMEOUT_SECONDS + "s");

        assertNull(receivedError.get(),
                "No debe haber error en login correcto. Error: " + receivedError.get());
        assertNotNull(receivedResponse.get());
        assertEquals("jwt-abc123", receivedResponse.get().getToken());
        assertEquals("ADMIN", receivedResponse.get().getRole());
        assertEquals("christian", receivedResponse.get().getUsername());

        assertTrue(SessionManager.getInstance().isLoggedIn());
        assertEquals("christian", SessionManager.getInstance().getUsername());
        assertEquals("ADMIN", SessionManager.getInstance().getRole());
    }

    @Test
    @DisplayName("a) Login correcto con rol USER: sesion activa con rol USER")
    void testLogin_withValidCredentials_userRole_startsSession() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"tok-user-789\",\"role\":\"USER\",\"username\":\"erik\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        authService.login("erik", "mypass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) {
                success.set(true);
                latch.countDown();
            }
            @Override public void onError(String errorMessage) { latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando respuesta de login USER");
        assertTrue(success.get(), "El callback onSuccess no fue invocado");
        assertTrue(SessionManager.getInstance().isLoggedIn());
        assertFalse(SessionManager.getInstance().isAdmin());
    }

    // =========================================================================
    // PRUEBA b): Login incorrecto — credenciales inválidas → error controlado
    // =========================================================================

    @Test
    @DisplayName("b) Login incorrecto: HTTP 401 -> 'Usuario o contrasena incorrectos'")
    void testLogin_withInvalidCredentials_http401_returnsCorrectMessage() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        authService.login("wronguser", "wrongpass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) { latch.countDown(); }
            @Override public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando respuesta de error 401");
        assertNotNull(errorMsg.get(), "Debe existir mensaje de error para HTTP 401");
        assertEquals("Usuario o contraseña incorrectos", errorMsg.get());
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("b) Login incorrecto: HTTP 403 -> 'Tu cuenta esta desactivada'")
    void testLogin_http403_returnsAccountDisabledMessage() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(403));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        authService.login("disabled_user", "pass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) { latch.countDown(); }
            @Override public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando respuesta de error 403");
        assertEquals("Tu cuenta esta desactivada", errorMsg.get());
    }

    @Test
    @DisplayName("b) Login incorrecto: HTTP 500 -> 'Error en el servidor'")
    void testLogin_http500_returnsServerErrorMessage() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        authService.login("user", "pass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) { latch.countDown(); }
            @Override public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando respuesta de error 500");
        assertEquals("Error en el servidor", errorMsg.get());
    }

    // =========================================================================
    // PRUEBA c): Logout — cierre de sesión correcto
    // =========================================================================

    @Test
    @DisplayName("c) Logout correcto: HTTP 204 -> sesion local limpiada")
    void testLogout_http204_clearsLocalSession() throws InterruptedException {
        SessionManager.getInstance().startSession("tok-xyz", "USER", "erik");
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        CountDownLatch latch = new CountDownLatch(1);
        authService.logout(latch::countDown);

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando finalizacion del logout");
        assertFalse(SessionManager.getInstance().isLoggedIn(),
                "La sesion debe estar cerrada tras logout correcto");
        assertNull(SessionManager.getInstance().getToken());
    }

    @Test
    @DisplayName("c) Logout con error de servidor: sesion local se limpia igualmente")
    void testLogout_whenServerReturnsError_stillClearsLocalSession() throws InterruptedException {
        SessionManager.getInstance().startSession("tok-fail", "USER", "erik");
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        CountDownLatch latch = new CountDownLatch(1);
        authService.logout(latch::countDown);

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout esperando finalizacion del logout con error de servidor");
        assertFalse(SessionManager.getInstance().isLoggedIn(),
                "La sesion local debe limpiarse incluso si el servidor devuelve error");
    }

    @Test
    @DisplayName("c) Logout: callback onComplete siempre se ejecuta")
    void testLogout_onCompleteAlwaysExecuted() throws InterruptedException {
        SessionManager.getInstance().startSession("tok", "ADMIN", "christian");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);

        authService.logout(() -> {
            callbackExecuted.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout: el callback onComplete no se ejecuto");
        assertTrue(callbackExecuted.get(),
                "El callback onComplete debe ejecutarse siempre tras logout");
    }

    // =========================================================================
    // PRUEBA d): Acceso a pantalla según tipo de usuario
    // =========================================================================

    @Test
    @DisplayName("d) Login con rol ADMIN -> isAdmin() retorna true -> abre DashboardAgentGUI")
    void testLogin_adminRole_sessionManagerReflectsAdminTrue() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"tok\",\"role\":\"ADMIN\",\"username\":\"christian\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        authService.login("christian", "pass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override public void onError(String e) { latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue(SessionManager.getInstance().isAdmin(),
                "isAdmin() debe ser true tras login con rol ADMIN -> DashboardAgentGUI");
    }

    @Test
    @DisplayName("d) Login con rol USER -> isAdmin() retorna false -> abre DashboardUserGUI")
    void testLogin_userRole_sessionManagerReflectsAdminFalse() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"tok\",\"role\":\"USER\",\"username\":\"erik\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        authService.login("erik", "pass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override public void onError(String e) { latch.countDown(); }
        });

        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse(SessionManager.getInstance().isAdmin(),
                "isAdmin() debe ser false tras login con rol USER -> DashboardUserGUI");
    }

    // =========================================================================
    // PRUEBA e): Cabecera Authorization en las peticiones
    // =========================================================================

    @Test
    @DisplayName("e) Logout envia cabecera Authorization: Bearer <token>")
    void testLogout_sendsAuthorizationBearerHeader() throws Exception {
        SessionManager.getInstance().startSession("my-bearer-token", "ADMIN", "christian");
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        CountDownLatch latch = new CountDownLatch(1);
        authService.logout(latch::countDown);
        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request,
                "MockWebServer debe haber recibido la peticion de logout");
        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader,
                "La peticion de logout debe incluir la cabecera Authorization");
        assertTrue(authHeader.startsWith("Bearer "),
                "La cabecera Authorization debe tener formato 'Bearer <token>'");
        assertEquals("Bearer my-bearer-token", authHeader,
                "El token en la cabecera debe coincidir con el de la sesion activa");
    }

    @Test
    @DisplayName("e) Login sin sesion previa: NO envia cabecera Authorization")
    void testLogin_doesNotSendAuthorizationHeader() throws Exception {
        SessionManager.getInstance().clearSession();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"token\":\"tok\",\"role\":\"USER\",\"username\":\"erik\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        authService.login("erik", "pass", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { latch.countDown(); }
            @Override public void onError(String e) { latch.countDown(); }
        });
        assertTrue(latch.await(ASYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request,
                "MockWebServer debe haber recibido la peticion de login");
        assertNull(request.getHeader("Authorization"),
                "La peticion de login no debe incluir Authorization cuando no hay sesion activa");
    }

    // =========================================================================
    // Prueba especial: SERVIDOR APAGADO
    // =========================================================================

    @Test
    @DisplayName("SERVIDOR APAGADO: login falla con 'No ha sido posible conectar'")
    void testLogin_whenServerIsDown_callsOnErrorWithConnectionMessage() throws Exception {
        // Apagar el servidor antes de hacer login para simular que está caído
        mockWebServer.shutdown();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorMsg = new AtomicReference<>();

        authService.login("christian", "pass123", new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse response) { latch.countDown(); }
            @Override public void onError(String errorMessage) {
                errorMsg.set(errorMessage);
                latch.countDown();
            }
        });

        // Timeout mayor para dar tiempo al fallo de conexión de OkHttp
        assertTrue(latch.await(15, TimeUnit.SECONDS),
                "Timeout esperando fallo de conexion con servidor apagado");
        assertNotNull(errorMsg.get(),
                "Debe existir mensaje de error cuando el servidor esta apagado");
        assertTrue(errorMsg.get().startsWith("No ha sido posible conectar"),
                "Mensaje recibido: " + errorMsg.get());
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }
}