package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la logica interna de {@link StatsPanel}.
 * Los metodos privados se invocan mediante reflexion.
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StatsPanel - Pruebas unitarias")
class StatsPanelLogicTest {

    private StatsPanel agentPanel;

    @BeforeAll
    void setUp() {
        System.setProperty("java.awt.headless", "true");
        SessionManager.getInstance().startSession("tok", "ADMIN", "cristina");
        agentPanel = new StatsPanel(() -> {}, false);
    }

    @AfterAll
    void cleanup() { SessionManager.getInstance().clearSession(); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Object invoke(String name, Class<?>[] types, Object[] args) throws Exception {
        Method m = StatsPanel.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(agentPanel, args);
    }

    private void setFiltered(List<TicketDTO> list) throws Exception {
        Field f = StatsPanel.class.getDeclaredField("filteredTickets");
        f.setAccessible(true);
        f.set(agentPanel, new ArrayList<>(list));
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

    private TicketDTO dto(String status, String priority,
                          String createdAt, String resolvedAt, String createdBy) {
        TicketDTO t = new TicketDTO();
        t.setStatus(status); t.setPriority(priority);
        t.setCreatedAt(createdAt); t.setResolvedAt(resolvedAt);
        t.setCreatedBy(createdBy);
        return t;
    }

    // =========================================================================
    // T01 — parseDateTime
    // =========================================================================

    @Test @Order(1)
    @DisplayName("T01 - parseDateTime: multiples formatos ISO; null e invalido -> null")
    void testParseDateTime() throws Exception {
        LocalDateTime r1 = (LocalDateTime) invoke("parseDateTime",
                new Class[]{String.class}, new Object[]{"2026-04-19T14:45:49.547151"});
        assertNotNull(r1, "Debe parsear formato con microsegundos");
        assertEquals(2026, r1.getYear());

        LocalDateTime r2 = (LocalDateTime) invoke("parseDateTime",
                new Class[]{String.class}, new Object[]{"2026-01-15T09:30:00"});
        assertNotNull(r2, "Debe parsear formato basico");

        assertNull(invoke("parseDateTime", new Class[]{String.class},
                new Object[]{(String) null}), "null -> null");
        assertNull(invoke("parseDateTime", new Class[]{String.class},
                new Object[]{"no-es-fecha"}), "invalido -> null");
    }

    // =========================================================================
    // T02 — isActiveStatus
    // =========================================================================

    @Test @Order(2)
    @DisplayName("T02 - isActiveStatus: Opened/In Progress/Pending activos; Solved/Closed no")
    void testIsActiveStatus() throws Exception {
        assertTrue((boolean)  invoke("isActiveStatus", new Class[]{String.class}, new Object[]{"Opened"}));
        assertTrue((boolean)  invoke("isActiveStatus", new Class[]{String.class}, new Object[]{"In Progress"}));
        assertTrue((boolean)  invoke("isActiveStatus", new Class[]{String.class}, new Object[]{"Pending"}));
        assertFalse((boolean) invoke("isActiveStatus", new Class[]{String.class}, new Object[]{"Solved"}));
        assertFalse((boolean) invoke("isActiveStatus", new Class[]{String.class}, new Object[]{"Closed"}));
    }

    // =========================================================================
    // T03 — isOlderThan4h
    // =========================================================================

    @Test @Order(3)
    @DisplayName("T03 - isOlderThan4h: >4h es overdue; <4h no; null -> false")
    void testIsOlderThan4h() throws Exception {
        String fmt = "yyyy-MM-dd'T'HH:mm:ss";
        assertTrue((boolean) invoke("isOlderThan4h", new Class[]{String.class},
                new Object[]{LocalDateTime.now().minusHours(5).format(DateTimeFormatter.ofPattern(fmt))}),
                "5h -> overdue");
        assertFalse((boolean) invoke("isOlderThan4h", new Class[]{String.class},
                new Object[]{LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ofPattern(fmt))}),
                "1h -> no overdue");
        assertFalse((boolean) invoke("isOlderThan4h",
                new Class[]{String.class}, new Object[]{(String) null}), "null -> false");
    }

    // =========================================================================
    // T04 — countByStatus
    // =========================================================================

    @Test @Order(4)
    @DisplayName("T04 - countByStatus: cuenta por estado; lista vacia -> 0")
    void testCountByStatus() throws Exception {
        setFiltered(List.of(
                dto("Opened", null, null, null, null),
                dto("Opened", null, null, null, null),
                dto("In Progress", null, null, null, null),
                dto("Solved", null, null, null, null)
        ));
        assertEquals(2L, invoke("countByStatus", new Class[]{String.class}, new Object[]{"Opened"}));
        assertEquals(1L, invoke("countByStatus", new Class[]{String.class}, new Object[]{"Solved"}));
        assertEquals(0L, invoke("countByStatus", new Class[]{String.class}, new Object[]{"Closed"}));

        setFiltered(new ArrayList<>());
        assertEquals(0L, invoke("countByStatus", new Class[]{String.class}, new Object[]{"Opened"}),
                "Lista vacia -> 0");
    }

    // =========================================================================
    // T05 — countByPriority
    // =========================================================================

    @Test @Order(5)
    @DisplayName("T05 - countByPriority: cuenta correctamente por prioridad")
    void testCountByPriority() throws Exception {
        setFiltered(List.of(
                dto("Opened", "HIGH",   null, null, null),
                dto("Opened", "HIGH",   null, null, null),
                dto("Opened", "MEDIUM", null, null, null)
        ));
        assertEquals(2L, invoke("countByPriority", new Class[]{String.class}, new Object[]{"HIGH"}));
        assertEquals(1L, invoke("countByPriority", new Class[]{String.class}, new Object[]{"MEDIUM"}));
        assertEquals(0L, invoke("countByPriority", new Class[]{String.class}, new Object[]{"CRITICAL"}));
    }

    // =========================================================================
    // T06 — calcAvgResolutionHours
    // =========================================================================

    @Test @Order(6)
    @DisplayName("T06 - calcAvgResolutionHours: calcula promedio; ignora Opened; -1 sin datos")
    void testCalcAvgResolutionHours() throws Exception {
        setFiltered(new ArrayList<>());
        assertEquals(-1.0, invoke("calcAvgResolutionHours", new Class[0], new Object[0]),
                "Lista vacia -> -1");

        setFiltered(List.of(dto("Solved", "HIGH", "2026-04-19T10:00:00", null, null)));
        assertEquals(-1.0, invoke("calcAvgResolutionHours", new Class[0], new Object[0]),
                "Sin resolvedAt -> -1");

        setFiltered(List.of(
                dto("Opened", "HIGH",   "2026-04-19T10:00:00", "2026-04-19T22:00:00", null),
                dto("Solved", "HIGH",   "2026-04-19T10:00:00", "2026-04-19T12:00:00", null),
                dto("Closed", "MEDIUM", "2026-04-19T08:00:00", "2026-04-19T14:00:00", null)
        ));
        assertEquals(4.0, (double) invoke("calcAvgResolutionHours", new Class[0], new Object[0]),
                0.01, "Promedio (2h+6h)/2=4h; Opened ignorado");
    }

    // =========================================================================
    // T07 — applyFilter
    // =========================================================================

    @Test @Order(7)
    @DisplayName("T07 - applyFilter: modo usuario filtra propios; agente ALL devuelve todos")
    void testApplyFilter() throws Exception {
        Method applyFilter = StatsPanel.class.getDeclaredMethod("applyFilter");
        applyFilter.setAccessible(true);

        StatsPanel userPanel = new StatsPanel(() -> {}, true);
        setAll(userPanel, List.of(
                dto("Opened", "HIGH", null, null, "cristina"),
                dto("Opened", "LOW",  null, null, "cristina"),
                dto("Opened", "HIGH", null, null, "otroUsuario")
        ));
        applyFilter.invoke(userPanel);
        List<TicketDTO> userResult = getFiltered(userPanel);
        assertEquals(2, userResult.size(), "Solo los 2 tickets de cristina");
        assertTrue(userResult.stream().allMatch(t -> "cristina".equals(t.getCreatedBy())));

        setAll(agentPanel, List.of(
                dto("Opened", "HIGH", null, null, "u1"),
                dto("Solved", "LOW",  null, null, "u2"),
                dto("Closed", "HIGH", null, null, "u3")
        ));
        applyFilter.invoke(agentPanel);
        assertEquals(3, getFiltered(agentPanel).size(), "Agente ALL: 3 tickets");
    }

    // =========================================================================
    // T08 — ticketsByDay
    // =========================================================================

    @Test @Order(8)
    @DisplayName("T08 - ticketsByDay: 7 entradas siempre; hoy cuenta; hace 8 dias no")
    void testTicketsByDay() throws Exception {
        String fmt = "yyyy-MM-dd'T'HH:mm:ss";
        setFiltered(new ArrayList<>());
        LinkedHashMap<?, ?> empty = (LinkedHashMap<?, ?>) invoke(
                "ticketsByDay", new Class[0], new Object[0]);
        assertEquals(7, empty.size(), "Siempre 7 entradas");

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern(fmt));
        String old   = LocalDateTime.now().minusDays(8).format(DateTimeFormatter.ofPattern(fmt));
        setFiltered(List.of(
                dto("Opened", "HIGH", today, null, null),
                dto("Opened", "HIGH", today, null, null),
                dto("Opened", "HIGH", old,   null, null)
        ));
        LinkedHashMap<String, Long> result = (LinkedHashMap<String, Long>) invoke(
                "ticketsByDay", new Class[0], new Object[0]);
        long total = result.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(2, total, "2 de hoy cuentan; el de hace 8 dias no");
    }
}
