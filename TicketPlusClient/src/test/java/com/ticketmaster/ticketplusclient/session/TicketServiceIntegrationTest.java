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
import static org.junit.jupiter.api.Assumptions.*;
 
/**
 * Pruebas de integracion con red para {@link TicketService}.
 *
 * <p>Conecta al servidor real en {@code http://10.2.99.25:8080/}.
 * El login se realiza UNA SOLA VEZ en {@code @BeforeAll}.</p>
 *
 * <p>El {@code @BeforeAll} es tolerante al fallo de conexion:
 * <ul>
 *   <li>Si el servidor esta <b>encendido</b>: {@code serverAvailable = true},
 *       T1-T8 se ejecutan y T9 se salta automaticamente.</li>
 *   <li>Si el servidor esta <b>apagado</b>: {@code serverAvailable = false},
 *       T1-T8 se saltan y T9 se ejecuta demostrando el fallo controlado.</li>
 * </ul>
 * No es necesario cambiar ningun codigo entre una ejecucion y otra.</p>
 *
 * <p>Historias de usuario cubiertas:
 * <ul>
 *   <li>#84 Listado de todos los tickets (filtro cliente)</li>
 *   <li>#85 Asignar ticket</li>
 *   <li>#86 Cambiar estado de un ticket</li>
 *   <li>#87 Responder un ticket (comentarios)</li>
 * </ul>
 * </p>
 *
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TicketService - Pruebas de integracion con servidor REAL")
class TicketServiceIntegrationTest {
 
    // -------------------------------------------------------------------------
    // Credenciales — ajustar a las del servidor real
    // -------------------------------------------------------------------------
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String AGENT_USER = "user1";
 
    private static final int TIMEOUT = 20;
 
    // -------------------------------------------------------------------------
    // Estado compartido
    // -------------------------------------------------------------------------
    private TicketService ticketService;
    private AuthService   authService;
    private Long          testTicketId;
 
    /**
     * true si el servidor respondio correctamente en @BeforeAll.
     * T1-T8: assumeTrue(serverAvailable) — se saltan si el servidor esta apagado.
     * T9:    assumeFalse(serverAvailable) — solo se ejecuta si el servidor esta apagado.
     */
    private boolean serverAvailable = false;
 
    // =========================================================================
    // Setup / Teardown
    // =========================================================================
 
    /**
     * Intenta hacer login antes de todos los tests.
     * Si el servidor no esta disponible, NO lanza excepcion:
     * simplemente deja serverAvailable = false y retorna.
     * Esto permite que T9 se ejecute aunque el servidor este apagado.
     */
    @BeforeAll
    void loginAndPrepare() throws InterruptedException {
        authService   = new AuthService();
        ticketService = new TicketService();
 
        // Intentar login — tolerante al fallo
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();
 
        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { loginLatch.countDown(); }
            @Override public void onError(String e)          { loginError.set(e); loginLatch.countDown(); }
        });
 
        boolean respondedInTime = loginLatch.await(TIMEOUT, TimeUnit.SECONDS);
 
        if (!respondedInTime || loginError.get() != null) {
            System.out.println("[BeforeAll] Servidor no disponible. "
                    + "T1-T8 se saltaran. T9 demostrara el fallo controlado.");
            return; // serverAvailable permanece false
        }
 
        serverAvailable = true;
        System.out.println("[BeforeAll] Login OK - usuario: "
                + SessionManager.getInstance().getUsername());
 
        // Obtener ticket de prueba para T3-T8
        CountDownLatch ticketLatch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> tickets = new AtomicReference<>();
 
        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> list) { tickets.set(list); ticketLatch.countDown(); }
            @Override public void onError(String e)               { ticketLatch.countDown(); }
        });
 
        if (ticketLatch.await(TIMEOUT, TimeUnit.SECONDS)
                && tickets.get() != null
                && !tickets.get().isEmpty()) {
            testTicketId = tickets.get().stream()
                    .filter(t -> "Opened".equals(t.getStatus()))
                    .findFirst()
                    .map(TicketDTO::getId)
                    .orElse(tickets.get().get(0).getId());
            System.out.println("[BeforeAll] Ticket de prueba seleccionado - id: " + testTicketId);
        }
    }
 
    @AfterAll
    void cleanup() {
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();
        System.out.println("[AfterAll] Sesion cerrada y ClientAPI reseteado.");
    }
 
    // =========================================================================
    // HU #84 — Listado de todos los tickets con filtros
    // =========================================================================
 
    @Test
    @Order(1)
    @DisplayName("#84 Listado: ADMIN recibe lista completa de tickets [SERVIDOR ENCENDIDO]")
    void testGetTickets_adminReceivesFullList() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
 
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
    @DisplayName("#84 Filtro cliente: countByStatus coherente con la lista del servidor")
    void testFilterByStatus_clientSide_countIsConsistent() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
 
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
 
        assertTrue(openCount + progressCount <= all.size(),
                "La suma de filtros no puede superar el total");
 
        System.out.println("Total: " + all.size()
                + " | Opened: " + openCount + " | In Progress: " + progressCount);
    }
 
    // =========================================================================
    // HU #85 — Asignar ticket
    // =========================================================================
 
    @Test
    @Order(3)
    @DisplayName("#85 Asignar: assignToMe asigna el ticket al ADMIN autenticado [SERVIDOR ENCENDIDO]")
    void testAssignToMe_updatesAgentToCurrentAdmin() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
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
        assertEquals(ADMIN_USER, result.get().getAgent(),
                "El agente debe ser el usuario ADMIN autenticado");
 
        System.out.println("Ticket " + result.get().getRef() + " asignado a: " + result.get().getAgent());
    }
 
    @Test
    @Order(4)
    @DisplayName("#85 Asignar: assignToAgent asigna a un agente especifico [SERVIDOR ENCENDIDO]")
    void testAssignToAgent_updatesAgentToSpecifiedUsername() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
        assertNotNull(testTicketId);
 
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
 
        ticketService.assignToAgent(testTicketId, AGENT_USER, new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(TicketDTO t) { result.set(t); latch.countDown(); }
            @Override public void onError(String e)      { error.set(e);  latch.countDown(); }
        });
 
        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS), "Timeout assignToAgent");
        assertNull(error.get(), "Error al asignar agente: " + error.get());
        assertNotNull(result.get());
        assertEquals(AGENT_USER, result.get().getAgent(),
                "El agente debe ser el username enviado");
 
        System.out.println("Ticket " + result.get().getRef()
                + " asignado a agente: " + result.get().getAgent());
    }
 
    // =========================================================================
    // HU #86 — Cambiar estado de un ticket
    // =========================================================================
 
    @Test
    @Order(5)
    @DisplayName("#86 Estado: changeStatus cambia el estado a 'In Progress' [SERVIDOR ENCENDIDO]")
    void testChangeStatus_toInProgress() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
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
        assertEquals("In Progress", result.get().getStatus());
 
        System.out.println("Ticket " + result.get().getRef()
                + " - nuevo estado: " + result.get().getStatus());
    }
 
    @Test
    @Order(6)
    @DisplayName("#86 Estado: changeStatus cambia el estado a 'Solved' [SERVIDOR ENCENDIDO]")
    void testChangeStatus_toSolved() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
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
        assertEquals("Solved", result.get().getStatus());
    }
 
    // =========================================================================
    // HU #87 — Responder un ticket (comentarios)
    // =========================================================================
 
    @Test
    @Order(7)
    @DisplayName("#87 Comentario: addComment crea el comentario y lo devuelve [SERVIDOR ENCENDIDO]")
    void testAddComment_createsCommentSuccessfully() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
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
        assertNotNull(result.get().getId());
        assertEquals(content, result.get().getContent());
        assertNotNull(result.get().getAuthor());
 
        System.out.println("Comentario creado - id: " + result.get().getId()
                + " | autor: " + result.get().getAuthor());
    }
 
    @Test
    @Order(8)
    @DisplayName("#87 Comentario: getComments devuelve el hilo de comentarios [SERVIDOR ENCENDIDO]")
    void testGetComments_returnsCommentThread() throws InterruptedException {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
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
        assertFalse(result.get().isEmpty());
 
        CommentDTO first = result.get().get(0);
        assertNotNull(first.getContent());
        assertNotNull(first.getAuthor());
        assertNotNull(first.getCreatedAt());
 
        System.out.println("Comentarios del ticket: " + result.get().size());
    }
 
    // =========================================================================
    // Prueba especial — Servidor apagado
    // =========================================================================
 
    /**
     * Verifica el comportamiento del cliente cuando el servidor esta apagado.
     *
     * <p>Este test se activa AUTOMATICAMENTE cuando se ejecuta con el servidor
     * apagado. {@code assumeFalse(serverAvailable)} hace que se salte si el
     * servidor esta encendido, y se ejecute si esta apagado.</p>
     *
     * <p>Para ejecutarlo, simplemente apaga el servidor y lanza:
     * <pre>mvn test</pre>
     * El resultado esperado es:
     * <ul>
     *   <li>T1-T8: Skipped (servidor no disponible)</li>
     *   <li>T9: PASSED (fallo controlado demostrado)</li>
     * </ul>
     * </p>
     */
    @Test
    @Order(9)
    @DisplayName("SERVIDOR APAGADO: getTickets falla con mensaje de conexion [ACTIVADO AUTOMATICAMENTE]")
    void testGetTickets_whenServerIsDown_callsOnError() throws InterruptedException {
        assumeFalse(serverAvailable,
                "Servidor disponible - este test solo se ejecuta con el servidor APAGADO");
 
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
                "El cliente quedo bloqueado - onError nunca se invoco");
        assertFalse(successCalled.get(),
                "onSuccess NO debe invocarse con el servidor apagado");
        assertNotNull(error.get());
        assertTrue(error.get().contains("Sin conexion") || error.get().contains("conexion")
                || error.get().contains("connect"),
                "El mensaje debe indicar fallo de conexion. Recibido: " + error.get());
 
        System.out.println("Fallo controlado verificado. Mensaje: " + error.get());
    }
}
