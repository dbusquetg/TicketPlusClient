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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Pruebas de integracion con servidor REAL para la logica de ordenacion
 * por puntos de usuario en {@link TicketListPanel#sortFiltered()}.
 *
 * <p>Todos los tests usan tickets cargados del servidor en {@code @BeforeAll},
 * convertidos a {@link TicketListPanel.TicketRow}. Ningun test usa datos
 * fabricados localmente.</p>
 *
 * <p>Patron serverAvailable:
 * <ul>
 *   <li>Servidor <b>encendido</b>: T09-T15 se ejecutan, T16 se salta.</li>
 *   <li>Servidor <b>apagado</b>: T09-T15 se saltan, T16 se ejecuta.</li>
 * </ul>
 * </p>
 *
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TicketListPanel - Pruebas de integracion de ordenacion con servidor REAL")
class TicketListSortFilterTest {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final int    TIMEOUT    = 20;

    private TicketListPanel panel;
    private TicketService   ticketService;
    private boolean         serverAvailable = false;

    private Object OLDEST_FIRST;
    private Object NEWEST_FIRST;

    /** TicketRows reales convertidos desde el servidor en @BeforeAll. */
    private List<TicketListPanel.TicketRow> realRows = new ArrayList<>();

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeAll
    void loginAndLoadTickets() throws Exception {
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
            System.out.println("[BeforeAll] Servidor no disponible. T09-T15 se saltaran. T16 activo.");
            return;
        }

        // Cargar tickets reales y convertir a TicketRows
        CountDownLatch ticketLatch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> loaded = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override public void onSuccess(List<TicketDTO> list) { loaded.set(list); ticketLatch.countDown(); }
            @Override public void onError(String e) { ticketLatch.countDown(); }
        });

        if (ticketLatch.await(TIMEOUT, TimeUnit.SECONDS)
                && loaded.get() != null && !loaded.get().isEmpty()) {

            for (TicketDTO dto : loaded.get()) {
                int points = dto.getPoints() > 0
                        ? dto.getPoints()
                        : 100; // default hasta que el backend implemente el campo
                realRows.add(new TicketListPanel.TicketRow(
                        dto.getId(),
                        dto.getRef(),
                        dto.getTitle()       != null ? dto.getTitle()       : "",
                        dto.getDescription() != null ? dto.getDescription() : "",
                        dto.getPriority()    != null ? dto.getPriority()    : "MEDIUM",
                        dto.getStatus()      != null ? dto.getStatus()      : "Opened",
                        dto.getCreatedBy()   != null ? dto.getCreatedBy()   : "",
                        dto.getAgent()       != null ? dto.getAgent()       : "Sin asignar",
                        dto.getCreatedAt(),
                        points
                ));
            }

            panel = new TicketListPanel(() -> {}, id -> {});

            // Obtener constantes del enum SortOrder via reflexion
            for (Class<?> cls : TicketListPanel.class.getDeclaredClasses()) {
                if (cls.isEnum() && cls.getSimpleName().equals("SortOrder")) {
                    OLDEST_FIRST = cls.getEnumConstants()[0];
                    NEWEST_FIRST = cls.getEnumConstants()[1];
                }
            }

            serverAvailable = true;
            System.out.println("[BeforeAll] TicketRows reales cargados: " + realRows.size());
        }
    }

    @AfterAll
    void cleanup() {
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();
    }

    // ── Helpers de reflexion ──────────────────────────────────────────────────

    private void setFiltered(List<TicketListPanel.TicketRow> list) throws Exception {
        Field f = TicketListPanel.class.getDeclaredField("filtered");
        f.setAccessible(true);
        f.set(panel, new ArrayList<>(list));
    }

    private List<TicketListPanel.TicketRow> getFiltered() throws Exception {
        Field f = TicketListPanel.class.getDeclaredField("filtered");
        f.setAccessible(true);
        return (List<TicketListPanel.TicketRow>) f.get(panel);
    }

    private void setSortOrder(Object order) throws Exception {
        Field f = TicketListPanel.class.getDeclaredField("sortOrder");
        f.setAccessible(true);
        f.set(panel, order);
    }

    private void callSortFiltered() throws Exception {
        Method m = TicketListPanel.class.getDeclaredMethod("sortFiltered");
        m.setAccessible(true);
        m.invoke(panel);
    }

    private LocalDateTime callParseDate(String raw) throws Exception {
        Method m = TicketListPanel.class.getDeclaredMethod("parseTicketDate", String.class);
        m.setAccessible(true);
        return (LocalDateTime) m.invoke(panel, raw);
    }

    // =========================================================================
    // T09 — parseTicketDate con fechas reales del servidor
    // =========================================================================

    @Test @Order(9)
    @DisplayName("T09 - parseTicketDate: parsea todas las fechas reales del servidor sin excepcion")
    void testParseTicketDate() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        for (TicketListPanel.TicketRow row : realRows) {
            if (row.createdAt != null && !row.createdAt.isEmpty()) {
                LocalDateTime result = callParseDate(row.createdAt);
                assertNotNull(result,
                        "parseTicketDate debe parsear la fecha real: " + row.createdAt
                        + " del ticket " + row.ref);
            }
        }

        // null siempre devuelve null
        assertNull(callParseDate(null));

        System.out.println("Fechas parseadas: " + realRows.size());
    }

    // =========================================================================
    // T10 — sortFiltered OLDEST con datos reales: tamano y orden por puntos
    // =========================================================================

    @Test @Order(10)
    @DisplayName("T10 - sortFiltered OLDEST: tamano invariante; primer ticket >= puntos que ultimo")
    void testSortFiltered_oldestFirst_realData() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setSortOrder(OLDEST_FIRST);
        setFiltered(realRows);
        callSortFiltered();

        List<TicketListPanel.TicketRow> sorted = getFiltered();

        assertEquals(realRows.size(), sorted.size(),
                "sortFiltered no debe perder ni duplicar tickets");

        if (sorted.size() > 1) {
            int firstPts = sorted.get(0).createdByPoints;
            int lastPts  = sorted.get(sorted.size() - 1).createdByPoints;
            assertTrue(firstPts >= lastPts,
                    "OLDEST_FIRST: primer ticket debe tener >= puntos que el ultimo. "
                    + "Primero: " + firstPts + "pts | Ultimo: " + lastPts + "pts");
        }

        System.out.println("OLDEST - Primero: " + sorted.get(0).ref
                + " (" + sorted.get(0).createdByPoints + "pts)"
                + " | Ultimo: " + sorted.get(sorted.size()-1).ref
                + " (" + sorted.get(sorted.size()-1).createdByPoints + "pts)");
    }

    // =========================================================================
    // T11 — sortFiltered NEWEST con datos reales: tamano y orden por puntos
    // =========================================================================

    @Test @Order(11)
    @DisplayName("T11 - sortFiltered NEWEST: tamano invariante; primer ticket >= puntos que ultimo")
    void testSortFiltered_newestFirst_realData() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setSortOrder(NEWEST_FIRST);
        setFiltered(realRows);
        callSortFiltered();

        List<TicketListPanel.TicketRow> sorted = getFiltered();

        assertEquals(realRows.size(), sorted.size(),
                "sortFiltered no debe perder ni duplicar tickets");

        if (sorted.size() > 1) {
            int firstPts = sorted.get(0).createdByPoints;
            int lastPts  = sorted.get(sorted.size() - 1).createdByPoints;
            assertTrue(firstPts >= lastPts,
                    "NEWEST_FIRST: primer ticket debe tener >= puntos que el ultimo. "
                    + "Primero: " + firstPts + "pts | Ultimo: " + lastPts + "pts");
        }

        System.out.println("NEWEST - Primero: " + sorted.get(0).ref
                + " (" + sorted.get(0).createdByPoints + "pts)"
                + " | Ultimo: " + sorted.get(sorted.size()-1).ref
                + " (" + sorted.get(sorted.size()-1).createdByPoints + "pts)");
    }

    // =========================================================================
    // T12 — Desempate por fecha OLDEST: dentro de mismo grupo de puntos,
    //        fechas ascendentes
    // =========================================================================

    @Test @Order(12)
    @DisplayName("T12 - OLDEST: dentro del mismo grupo de puntos, fechas ordenadas ascendentemente")
    void testSort_oldestFirst_dateTiebreak() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setSortOrder(OLDEST_FIRST);
        setFiltered(realRows);
        callSortFiltered();

        List<TicketListPanel.TicketRow> sorted = getFiltered();

        // Verificar que dentro de cada grupo de puntos las fechas son ascendentes
        for (int i = 0; i < sorted.size() - 1; i++) {
            TicketListPanel.TicketRow curr = sorted.get(i);
            TicketListPanel.TicketRow next = sorted.get(i + 1);

            if (curr.createdByPoints == next.createdByPoints
                    && curr.createdAt != null && next.createdAt != null) {
                assertTrue(curr.createdAt.compareTo(next.createdAt) <= 0,
                        "OLDEST_FIRST: dentro del grupo de " + curr.createdByPoints + "pts, "
                        + curr.ref + " (" + curr.createdAt + ") debe ir antes que "
                        + next.ref + " (" + next.createdAt + ")");
            }
        }

        System.out.println("Desempate OLDEST verificado en " + sorted.size() + " tickets");
    }

    // =========================================================================
    // T13 — Desempate por fecha NEWEST: dentro de mismo grupo de puntos,
    //        fechas descendentes
    // =========================================================================

    @Test @Order(13)
    @DisplayName("T13 - NEWEST: dentro del mismo grupo de puntos, fechas ordenadas descendentemente")
    void testSort_newestFirst_dateTiebreak() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        setSortOrder(NEWEST_FIRST);
        setFiltered(realRows);
        callSortFiltered();

        List<TicketListPanel.TicketRow> sorted = getFiltered();

        // Verificar que dentro de cada grupo de puntos las fechas son descendentes
        for (int i = 0; i < sorted.size() - 1; i++) {
            TicketListPanel.TicketRow curr = sorted.get(i);
            TicketListPanel.TicketRow next = sorted.get(i + 1);

            if (curr.createdByPoints == next.createdByPoints
                    && curr.createdAt != null && next.createdAt != null) {
                assertTrue(curr.createdAt.compareTo(next.createdAt) >= 0,
                        "NEWEST_FIRST: dentro del grupo de " + curr.createdByPoints + "pts, "
                        + curr.ref + " (" + curr.createdAt + ") debe ir antes que "
                        + next.ref + " (" + next.createdAt + ")");
            }
        }

        System.out.println("Desempate NEWEST verificado en " + sorted.size() + " tickets");
    }

    // =========================================================================
    // T14 — Todos los puntos reales estan en el rango [0, 100]
    // =========================================================================

    @Test @Order(14)
    @DisplayName("T14 - Todos los tickets reales tienen createdByPoints en el rango [0, 100]")
    void testAllPointsInRange() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");

        for (TicketListPanel.TicketRow row : realRows) {
            assertTrue(row.createdByPoints >= 0 && row.createdByPoints <= 100,
                    "Ticket " + row.ref + " tiene puntos fuera de rango [0,100]: "
                    + row.createdByPoints);
        }

        int minPts = realRows.stream().mapToInt(r -> r.createdByPoints).min().orElse(0);
        int maxPts = realRows.stream().mapToInt(r -> r.createdByPoints).max().orElse(0);
        System.out.println("Rango de puntos reales: [" + minPts + ", " + maxPts + "]"
                + " en " + realRows.size() + " tickets");
    }

    // =========================================================================
    // T15 — TicketRow convertido desde ticket real del servidor
    // =========================================================================

    @Test @Order(15)
    @DisplayName("T15 - TicketRow: conversion correcta desde ticket real del servidor")
    void testTicketRow_fromRealData() throws Exception {
        assumeTrue(serverAvailable, "Servidor no disponible - test saltado");
        assertFalse(realRows.isEmpty(), "Debe haber al menos un ticket real del servidor");

        TicketListPanel.TicketRow first = realRows.get(0);

        assertNotNull(first.id,       "id no debe ser null");
        assertNotNull(first.ref,      "ref no debe ser null");
        assertFalse(first.ref.isEmpty(), "ref no debe estar vacia");
        assertTrue(first.createdByPoints >= 0 && first.createdByPoints <= 100,
                "createdByPoints debe estar en [0, 100]. Valor: " + first.createdByPoints);

        // El campo createdByPoints debe ser final
        assertTrue(
            java.lang.reflect.Modifier.isFinal(
                TicketListPanel.TicketRow.class
                    .getDeclaredField("createdByPoints").getModifiers()),
            "createdByPoints debe ser un campo final");

        System.out.println("Primer ticket real: " + first.ref
                + " | pts=" + first.createdByPoints
                + " | status=" + first.status);
    }

    // =========================================================================
    // T16 — Servidor apagado: fallo controlado
    // =========================================================================

    @Test @Order(16)
    @DisplayName("T16 - Servidor apagado: getTickets falla con mensaje de conexion [SERVIDOR APAGADO]")
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
