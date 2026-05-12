package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.model.LoginResponse;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.AuthService;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import com.ticketmaster.ticketplusclient.session.TicketService;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Pruebas de integracion con servidor REAL para la logica interna de
 * {@link StatsPanel}.
 *
 * <p>Todos los tests usan tickets cargados del servidor en {@code @BeforeAll}.
 * Ningun test usa datos fabricados localmente.</p>
 *
 * <p>Patron serverAvailable:
 * <ul>
 *   <li>Servidor <b>encendido</b>: T01-T08 se ejecutan, T09 se salta.</li>
 *   <li>Servidor <b>apagado</b>: T01-T08 se saltan, T09 se ejecuta.</li>
 * </ul>
 * </p>
 *
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StatsPanel - Pruebas de integracion con servidor REAL")
class StatsPanelLogicTest {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final int    TIMEOUT    = 20;

    private StatsPanel    agentPanel;
    private StatsPanel    userPanel;
    private TicketService ticketService;
    private boolean       serverAvailable = false;

    /** Tickets reales cargados del servidor en @BeforeAll. */
    private List<TicketDTO> realTickets = new ArrayList<>();

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeAll
    void loginAndLoadTickets() throws InterruptedException {
        System.setProperty("java.awt.headless", "true");
        ticketService = new TicketService();
        AuthService authService = new AuthService();

        // Login tolerante al fallo
        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> loginErr = new AtomicReference<>();

        authService.login(ADMIN_USER, ADMIN_PASS, new AuthService.AuthCallback() {
            @Override public void onSuccess(LoginResponse r) { loginLatch.countDown(); }
            @Override public void onError(String e) { loginErr.set(e); loginLatch.countDown(); }
        });

        boolean responded = loginLatch.await(TIMEOUT, TimeUnit.SECONDS);
        if (!responded || loginErr.get() != null) {
            System.out.println("[BeforeAll] Servidor no disponible. T01-T08 se saltaran. T09 activo.");
            return;
        }

        // Cargar tickets reales
        CountDownLatch ticketLatch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> loaded = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> list) { loaded.set(list); ticketLatch.countDown(); }
            @Override public void onError(String e) { ticketLatch.countDown(); }
        });

        if (ticketLatch.await(TIMEOUT, TimeUnit.SECONDS)
                && loaded.get() != null && !loaded.get().isEmpty()) {
            realTickets = loaded.get();
            serverAvailable = true;
            agentPanel = new StatsPanel(() -> {}, false);
            userPanel  = new StatsPanel(() -> {}, true);
            System.out.println("[BeforeAll] Tickets reales cargados: " + realTickets.size());
        }
    }

    @AfterAll
    void cleanup() {
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();
    }

    // ── Helpers de reflexion ──────────────────────────────────────────────────

    private Object invoke(StatsPanel p, String name,
                          Class<?>[] types, Object[] args) throws Exception {
        Method m = StatsPanel.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(p, args);
    }

    private void setFiltered(StatsPanel p, List<TicketDTO> list) throws Exception {
        Field f = StatsPanel.class.getDeclaredField("filteredTickets");
        f.setAccessible(true);
        f.set(p, new ArrayList<>(list));
    }

    private void setAll(StatsPanel p, List<TicketDTO> list) throws Exception {
        Field f = StatsPanel.class.getDeclaredField("allTickets");
        f.setAccessible(true);
        List<TicketDTO> field = (List<TicketDTO>) f.get(p);
        field.clear(); field.addAll(list);
    }

    private List<TicketDTO> getFiltered(StatsPanel p) throws Exception {
        Field f = StatsPanel.class.getDeclaredField("filteredTickets");
        f.setAccessible(true);
        return (List<TicketDTO>) f.get(p);
    }

    // =========================================================================
    // T01 — parseDateTime con fechas reales del servidor
    // =========================================================================

    @Test @Order(1)
    @DisplayName("T01 - parseDateTime: parsea correctamente las fechas reales del servidor")
    void testParseDateTime() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        // Todas las fechas reales del servidor deben parsearse sin excepcion
        for (TicketDTO ticket : realTickets) {
            String createdAt = ticket.getCreatedAt();
            if (createdAt != null && !createdAt.isEmpty()) {
                LocalDateTime result = (LocalDateTime) invoke(agentPanel, "parseDateTime",
                        new Class[]{String.class}, new Object[]{createdAt});
                assertNotNull(result,
                        "parseDateTime debe parsear la fecha real del servidor: " + createdAt);
            }
        }

        // null siempre devuelve null
        assertNull(invoke(agentPanel, "parseDateTime",
                new Class[]{String.class}, new Object[]{(String) null}));

        System.out.println("Fechas parseadas correctamente: " + realTickets.size());
    }

    // =========================================================================
    // T02 — isActiveStatus con estados reales del servidor
    // =========================================================================

    @Test @Order(2)
    @DisplayName("T02 - isActiveStatus: clasifica correctamente los estados reales del servidor")
    void testIsActiveStatus() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        long activos   = 0;
        long inactivos = 0;

        for (TicketDTO ticket : realTickets) {
            boolean active = (boolean) invoke(agentPanel, "isActiveStatus",
                    new Class[]{String.class}, new Object[]{ticket.getStatus()});
            if (active) activos++; else inactivos++;
        }

        // La suma de activos + inactivos debe ser el total
        assertEquals(realTickets.size(), activos + inactivos,
                "Todos los tickets deben clasificarse como activos o inactivos");

        // Verificar que los estados conocidos se clasifican correctamente
        assertFalse((boolean) invoke(agentPanel, "isActiveStatus",
                new Class[]{String.class}, new Object[]{"Solved"}), "Solved -> no activo");
        assertFalse((boolean) invoke(agentPanel, "isActiveStatus",
                new Class[]{String.class}, new Object[]{"Closed"}), "Closed -> no activo");
        assertTrue((boolean) invoke(agentPanel, "isActiveStatus",
                new Class[]{String.class}, new Object[]{"Opened"}), "Opened -> activo");

        System.out.println("Activos: " + activos + " | Inactivos: " + inactivos);
    }

    // =========================================================================
    // T03 — isOlderThan4h con fechas reales del servidor
    // =========================================================================

    @Test @Order(3)
    @DisplayName("T03 - isOlderThan4h: no lanza excepcion con ninguna fecha real del servidor")
    void testIsOlderThan4h() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        int overdue = 0;
        int notOverdue = 0;

        for (TicketDTO ticket : realTickets) {
            // No debe lanzar excepcion para ninguna fecha real
            boolean result = (boolean) invoke(agentPanel, "isOlderThan4h",
                    new Class[]{String.class}, new Object[]{ticket.getCreatedAt()});
            if (result) overdue++; else notOverdue++;
        }

        // La suma debe ser el total (ninguna fecha real causa excepcion)
        assertEquals(realTickets.size(), overdue + notOverdue,
                "isOlderThan4h debe procesar todas las fechas reales sin excepcion");

        System.out.println("Overdue (>4h): " + overdue + " | No overdue: " + notOverdue);
    }

    // =========================================================================
    // T04 — countByStatus con tickets reales del servidor
    // =========================================================================

    @Test @Order(4)
    @DisplayName("T04 - countByStatus: la suma de todos los estados coincide con el total real")
    void testCountByStatus() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setFiltered(agentPanel, realTickets);

        long opened     = (long) invoke(agentPanel, "countByStatus",
                new Class[]{String.class}, new Object[]{"Opened"});
        long inProgress = (long) invoke(agentPanel, "countByStatus",
                new Class[]{String.class}, new Object[]{"In Progress"});
        long pending    = (long) invoke(agentPanel, "countByStatus",
                new Class[]{String.class}, new Object[]{"Pending"});
        long solved     = (long) invoke(agentPanel, "countByStatus",
                new Class[]{String.class}, new Object[]{"Solved"});
        long closed     = (long) invoke(agentPanel, "countByStatus",
                new Class[]{String.class}, new Object[]{"Closed"});
        long total      = opened + inProgress + pending + solved + closed;

        assertEquals(realTickets.size(), total,
                "La suma de todos los estados debe ser igual al total de tickets del servidor. "
                + "Opened=" + opened + " InProgress=" + inProgress + " Pending=" + pending
                + " Solved=" + solved + " Closed=" + closed);

        System.out.println("Estados reales — Opened:" + opened + " InProgress:" + inProgress
                + " Pending:" + pending + " Solved:" + solved + " Closed:" + closed);
    }

    // =========================================================================
    // T05 — countByPriority con tickets reales del servidor
    // =========================================================================

    @Test @Order(5)
    @DisplayName("T05 - countByPriority: la suma de todas las prioridades coincide con el total real")
    void testCountByPriority() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setFiltered(agentPanel, realTickets);

        long high     = (long) invoke(agentPanel, "countByPriority",
                new Class[]{String.class}, new Object[]{"HIGH"});
        long medium   = (long) invoke(agentPanel, "countByPriority",
                new Class[]{String.class}, new Object[]{"MEDIUM"});
        long low      = (long) invoke(agentPanel, "countByPriority",
                new Class[]{String.class}, new Object[]{"LOW"});
        long critical = (long) invoke(agentPanel, "countByPriority",
                new Class[]{String.class}, new Object[]{"CRITICAL"});
        long total    = high + medium + low + critical;

        assertEquals(realTickets.size(), total,
                "La suma de todas las prioridades debe ser igual al total del servidor. "
                + "HIGH=" + high + " MEDIUM=" + medium + " LOW=" + low
                + " CRITICAL=" + critical);

        System.out.println("Prioridades reales — HIGH:" + high + " MEDIUM:" + medium
                + " LOW:" + low + " CRITICAL:" + critical);
    }

    // =========================================================================
    // T06 — calcAvgResolutionHours con tickets reales del servidor
    // =========================================================================

    @Test @Order(6)
    @DisplayName("T06 - calcAvgResolutionHours: resultado coherente con los datos reales del servidor")
    void testCalcAvgResolutionHours() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setFiltered(agentPanel, realTickets);

        double avg = (double) invoke(agentPanel, "calcAvgResolutionHours",
                new Class[0], new Object[0]);

        // El resultado debe ser -1 (sin datos resueltos) o positivo (hay tickets resueltos)
        assertTrue(avg == -1.0 || avg > 0.0,
                "calcAvgResolutionHours debe devolver -1.0 o un valor positivo. Recibido: " + avg);

        // Verificar si hay tickets Solved/Closed con resolvedAt
        long resueltos = realTickets.stream()
                .filter(t -> ("Solved".equals(t.getStatus()) || "Closed".equals(t.getStatus()))
                        && t.getResolvedAt() != null)
                .count();

        if (resueltos > 0) {
            assertTrue(avg > 0.0,
                    "Con " + resueltos + " tickets resueltos, el promedio debe ser > 0. Recibido: " + avg);
        } else {
            assertEquals(-1.0, avg,
                    "Sin tickets con resolvedAt, debe devolver -1.0");
        }

        System.out.println("Avg resolution: " + avg + "h | Tickets resueltos con fecha: " + resueltos);
    }

    // =========================================================================
    // T07 — applyFilter con tickets reales del servidor
    // =========================================================================

    @Test @Order(7)
    @DisplayName("T07 - applyFilter (agentMode ALL): devuelve todos los tickets reales del servidor")
    void testApplyFilter() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setAll(agentPanel, realTickets);

        Method applyFilter = StatsPanel.class.getDeclaredMethod("applyFilter");
        applyFilter.setAccessible(true);
        applyFilter.invoke(agentPanel);

        List<TicketDTO> result = getFiltered(agentPanel);

        assertEquals(realTickets.size(), result.size(),
                "Modo agente ALL debe devolver todos los tickets reales sin filtrar");

        System.out.println("applyFilter ALL: " + result.size() + " tickets (total real: "
                + realTickets.size() + ")");
    }

    // =========================================================================
    // T08 — ticketsByDay con tickets reales del servidor
    // =========================================================================

    @Test @Order(8)
    @DisplayName("T08 - ticketsByDay: 7 entradas; suma de valores coherente con datos reales")
    void testTicketsByDay() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setFiltered(agentPanel, realTickets);

        LinkedHashMap<String, Long> result = (LinkedHashMap<String, Long>)
                invoke(agentPanel, "ticketsByDay", new Class[0], new Object[0]);

        assertEquals(7, result.size(),
                "ticketsByDay debe devolver siempre 7 entradas");

        long sumDays = result.values().stream().mapToLong(Long::longValue).sum();
        assertTrue(sumDays <= realTickets.size(),
                "La suma de tickets por dia no puede superar el total real. "
                + "Suma: " + sumDays + " | Total: " + realTickets.size());

        System.out.println("Tickets ultimos 7 dias: " + sumDays
                + " de " + realTickets.size() + " totales");
    }

    // =========================================================================
    // T09 — Servidor apagado: fallo controlado
    // =========================================================================

    @Test @Order(9)
    @DisplayName("T09 - Servidor apagado: getTickets falla con mensaje de conexion [SERVIDOR APAGADO]")
    void testServerDown() throws InterruptedException {
        assumeFalse(serverAvailable,
                "Servidor disponible - este test solo se ejecuta con el servidor APAGADO");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> err = new AtomicReference<>();
        AtomicReference<Boolean> success = new AtomicReference<>(false);

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> t) { success.set(true); latch.countDown(); }
            @Override public void onError(String e) { err.set(e); latch.countDown(); }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS),
                "onError debe invocarse - el cliente no puede bloquearse indefinidamente");
        assertFalse(success.get(), "onSuccess NO debe invocarse con el servidor apagado");
        assertNotNull(err.get());
        String lower = err.get().toLowerCase();
        assertTrue(lower.contains("conex") || lower.contains("connect")
                || lower.contains("timeout") || lower.contains("timed out"),
                "Mensaje debe indicar fallo de conexion. Recibido: " + err.get());

        System.out.println("Fallo controlado verificado. Mensaje: " + err.get());
    }
}
