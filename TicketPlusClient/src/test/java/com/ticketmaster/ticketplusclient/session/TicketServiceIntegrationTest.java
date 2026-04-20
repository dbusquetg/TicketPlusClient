/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.session;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.model.CommentDTO;
import com.ticketmaster.ticketplusclient.model.LoginResponse;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración con red para {@link TicketService}.
 *
 * <p>Conecta al servidor real en {@code http://10.2.99.25:8080/}.
 * El login se realiza UNA SOLA VEZ en {@code @BeforeAll}, evitando
 * timeouts y logins repetidos entre tests.</p>
 *
 * <p>Historias de usuario cubiertas:
 * <ul>
 *   <li>#84 — Listado de todos los tickets (con filtro cliente)</li>
 *   <li>#85 — Asignar ticket</li>
 *   <li>#86 — Cambiar estado de un ticket</li>
 *   <li>#87 — Responder un ticket (comentarios)</li>
 * </ul>
 * </p>
 *
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TicketService - Pruebas de integración con servidor REAL")
class TicketServiceIntegrationTest {

    // -------------------------------------------------------------------------
    // Credenciales — ajustar a las del servidor real
    // -------------------------------------------------------------------------
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String AGENT_USER = "user1"; // agente existente en el servidor

    /** Timeout generoso para la primera petición (servidor frío). */
    private static final int TIMEOUT = 20;

    // -------------------------------------------------------------------------
    // Estado compartido — inicializado en @BeforeAll
    // -------------------------------------------------------------------------
    private TicketService ticketService;
    private AuthService   authService;

    /** ID del ticket "Opened" obtenido en @BeforeAll y reutilizado en T3–T8. */
    private Long testTicketId;

    // =========================================================================
    // Setup / Teardown
    // =========================================================================

    /**
     * Login ÚNICO antes de todos los tests. También obtiene el ID de un ticket
     * en estado "Opened" para reutilizarlo en las pruebas de asignación,
     * cambio de estado y comentarios.
     *
     * <p>Al usar {@code @TestInstance(PER_CLASS)}, este método puede ser
     * de instancia (no static) y tiene acceso a todos los campos.</p>
     *
     * @throws InterruptedException si el hilo es interrumpido
     */
    @BeforeAll
    void loginAndPrepare() throws InterruptedException {
        authService   = new AuthService();
        ticketService = new TicketService();

        // ── Login ──────────────────────────────────────────────────────────
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { loginLatch.countDown(); }
            @Override public void onError(String e)          { loginError.set(e); loginLatch.countDown(); }
        });

        assertTrue(loginLatch.await(TIMEOUT, TimeUnit.SECONDS),
                "Timeout en login inicial (" + TIMEOUT + "s). "
                + "Comprueba que el servidor esta encendido en http://10.2.99.25:8080/");
        assertNull(loginError.get(),
                "Login ADMIN fallido en @BeforeAll: " + loginError.get());

        System.out.println("Login OK — usuario: " + SessionManager.getInstance().getUsername());

        // ── Obtener ticket "Opened" para los tests ─────────────────────────
        CountDownLatch ticketLatch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> tickets = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> list) { tickets.set(list); ticketLatch.countDown(); }
            @Override public void onError(String e)               { ticketLatch.countDown(); }
        });

        assertTrue(ticketLatch.await(TIMEOUT, TimeUnit.SECONDS),
                "Timeout al obtener la lista de tickets en @BeforeAll");
        assertNotNull(tickets.get(), "La lista de tickets es null");
        assertFalse(tickets.get().isEmpty(), "El servidor no devolvio ningun ticket");

        // Preferir ticket "Opened"; si no hay, usar cualquiera
        testTicketId = tickets.get().stream()
                .filter(t -> "Opened".equals(t.getStatus()))
                .findFirst()
                .map(TicketDTO::getId)
                .orElse(tickets.get().get(0).getId());

        System.out.println("Ticket de prueba seleccionado — id: " + testTicketId);
    }

    /**
     * Limpieza después de todos los tests: cierra sesión y resetea ClientAPI.
     */
    @AfterAll
    void cleanup() {
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();
        System.out.println("Sesion cerrada y ClientAPI reseteado.");
    }

    // =========================================================================
    // US #84 — Listado de todos los tickets con filtros
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("#84 Listado: ADMIN recibe la lista completa de tickets del servidor [SERVIDOR ENCENDIDO]")
    void testGetTickets_adminReceivesFullList() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)            { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout getTickets");
        assertNull(error.get(), "Error al obtener tickets: " + error.get());
        assertNotNull(result.get());
        assertFalse(result.get().isEmpty(), "ADMIN debe recibir al menos un ticket");

        TicketDTO first = result.get().get(0);
        assertNotNull(first.getId(),     "Ticket debe tener id");
        assertNotNull(first.getRef(),    "Ticket debe tener referencia");
        assertNotNull(first.getStatus(), "Ticket debe tener estado");

        System.out.println("Tickets recibidos: " + result.get().size()
                + " | Primer ticket: " + first.getRef() + " [" + first.getStatus() + "]");
    }

    @Test
    @Order(2)
    @DisplayName("#84 Filtro cliente: countByStatus es coherente con la lista del servidor")
    void testFilterByStatus_clientSide_countIsConsistent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> result = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)            { latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
        assertNotNull(result.get());

        List<TicketDTO> all = result.get();
        long openCount     = all.stream().filter(t -> "Opened".equals(t.getStatus())).count();
        long progressCount = all.stream().filter(t -> "In Progress".equals(t.getStatus())).count();

        // La suma de filtros individuales no puede superar el total
        assertTrue(openCount + progressCount <= all.size(),
                "La suma de filtros no puede superar el total");

        System.out.println("Total: " + all.size()
                + " | Opened: " + openCount + " | In Progress: " + progressCount);
    }

    // =========================================================================
    // US #85 — Asignar ticket
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("#85 Asignar: assignToMe asigna el ticket al ADMIN autenticado [SERVIDOR ENCENDIDO]")
    void testAssignToMe_updatesAgentToCurrentAdmin() throws InterruptedException {
        assertNotNull(testTicketId, "testTicketId no fue inicializado en @BeforeAll");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.assignToMe(testTicketId, new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(TicketDTO t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)      { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout assignToMe");
        assertNull(error.get(), "Error al asignar ticket: " + error.get());
        assertNotNull(result.get());

        String agent = result.get().getAgent();
        assertNotNull(agent, "El ticket debe tener agente tras assignToMe");
        assertEquals(ADMIN_USER, agent,
                "El agente debe ser el usuario ADMIN autenticado");

        System.out.println("Ticket " + result.get().getRef() + " asignado a: " + agent);
    }

    @Test
    @Order(4)
    @DisplayName("#85 Asignar: assignToAgent asigna a un agente especifico [SERVIDOR ENCENDIDO]")
    void testAssignToAgent_updatesAgentToSpecifiedUsername() throws InterruptedException {
        assertNotNull(testTicketId);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.assignToAgent(testTicketId, AGENT_USER, new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(TicketDTO t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)      { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout assignToAgent");
        assertNull(error.get(), "Error al asignar agente especifico: " + error.get());
        assertNotNull(result.get());
        assertEquals(AGENT_USER, result.get().getAgent(),
                "El agente debe ser el username enviado en la peticion");

        System.out.println("Ticket " + result.get().getRef()
                + " asignado a agente: " + result.get().getAgent());
    }

    // =========================================================================
    // US #86 — Cambiar estado de un ticket
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("#86 Estado: changeStatus cambia el estado a 'In Progress' [SERVIDOR ENCENDIDO]")
    void testChangeStatus_toInProgress() throws InterruptedException {
        assertNotNull(testTicketId);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.changeStatus(testTicketId, "In Progress", new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(TicketDTO t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)      { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout changeStatus");
        assertNull(error.get(), "Error al cambiar estado: " + error.get());
        assertNotNull(result.get());
        assertEquals("In Progress", result.get().getStatus(),
                "El estado del ticket debe haberse actualizado a 'In Progress'");

        System.out.println("Ticket " + result.get().getRef()
                + " — nuevo estado: " + result.get().getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("#86 Estado: changeStatus cambia el estado a 'Solved' [SERVIDOR ENCENDIDO]")
    void testChangeStatus_toSolved() throws InterruptedException {
        assertNotNull(testTicketId);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.changeStatus(testTicketId, "Solved", new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(TicketDTO t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)      { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout changeStatus Solved");
        assertNull(error.get(), "Error al cambiar a Solved: " + error.get());
        assertNotNull(result.get());
        assertEquals("Solved", result.get().getStatus());
    }

    // =========================================================================
    // US #87 — Responder un ticket (comentarios)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("#87 Comentario: addComment crea el comentario y lo devuelve [SERVIDOR ENCENDIDO]")
    void testAddComment_createsCommentSuccessfully() throws InterruptedException {
        assertNotNull(testTicketId);

        String content = "Comentario de prueba automatizada - " + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommentDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.addComment(testTicketId, content, new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(CommentDTO c) { result.set(c); latch.countDown(); }
            @Override public void onError(String e)       { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout addComment");
        assertNull(error.get(), "Error al añadir comentario: " + error.get());
        assertNotNull(result.get());
        assertNotNull(result.get().getId(),     "El comentario debe tener id asignado por el servidor");
        assertEquals(content, result.get().getContent(),
                "El contenido del comentario debe coincidir con el enviado");
        assertNotNull(result.get().getAuthor(), "El comentario debe tener autor");

        System.out.println("Comentario creado — id: " + result.get().getId()
                + " | autor: " + result.get().getAuthor());
    }

    @Test
    @Order(8)
    @DisplayName("#87 Comentario: getComments devuelve el hilo de comentarios del ticket [SERVIDOR ENCENDIDO]")
    void testGetComments_returnsCommentThread() throws InterruptedException {
        assertNotNull(testTicketId);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<CommentDTO>> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.getComments(testTicketId, new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<CommentDTO> c) { result.set(c); latch.countDown(); }
            @Override public void onError(String e)             { error.set(e);  latch.countDown(); }
        });

        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout getComments");
        assertNull(error.get(), "Error al obtener comentarios: " + error.get());
        assertNotNull(result.get());
        assertFalse(result.get().isEmpty(),
                "Debe haber al menos el comentario añadido en el test anterior");

        CommentDTO first = result.get().get(0);
        assertNotNull(first.getContent(),   "Comentario debe tener contenido");
        assertNotNull(first.getAuthor(),    "Comentario debe tener autor");
        assertNotNull(first.getCreatedAt(), "Comentario debe tener fecha");

        System.out.println("Comentarios del ticket: " + result.get().size());
    }

    // =========================================================================
    // Prueba especial — Servidor apagado (ejecución MANUAL)
    // =========================================================================

    /**
     * Prueba de fallo controlado con servidor apagado.
     *
     * <p><b>Esta prueba esta deshabilitada intencionadamente.</b>
     * Para ejecutarla manualmente:</p>
     * <ol>
     *   <li>Apaga el servidor backend.</li>
     *   <li>Ejecuta solo este test:<br>
     *       {@code mvn test -Dtest=TicketServiceIntegrationTest#testGetTickets_whenServerIsDown_callsOnError}</li>
     *   <li>Verifica que el test PASA (recibe mensaje de conexion fallida).</li>
     * </ol>
     */
    @Test
    @Order(9)
    @Disabled("Ejecutar MANUALMENTE con el servidor APAGADO para demostrar fallo controlado")
    @DisplayName("SERVIDOR APAGADO: getTickets falla con mensaje de conexion [MANUAL — SERVIDOR APAGADO]")
    void testGetTickets_whenServerIsDown_callsOnError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        AtomicReference<Boolean> successCalled = new AtomicReference<>(false);

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> t) {
                successCalled.set(true);
                latch.countDown();
            }
            @Override public void onError(String e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS),
                "El cliente quedo bloqueado — onError nunca se invoco");
        assertFalse(successCalled.get(),
                "onSuccess NO debe invocarse con el servidor apagado");
        assertNotNull(error.get());
        assertTrue(error.get().contains("Sin conexion") || error.get().contains("conexion")
                || error.get().contains("connect"),
                "El mensaje debe indicar fallo de conexion. Recibido: " + error.get());
    }
}
