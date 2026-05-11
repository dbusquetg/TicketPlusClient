package com.ticketmaster.ticketplusclient.gui;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la logica de ordenacion por puntos de usuario
 * en {@link TicketListPanel#sortFiltered()}.
 * Los metodos privados se invocan mediante reflexion.
 * @author Christian
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TicketListPanel - Pruebas unitarias de ordenacion por puntos")
class TicketListSortFilterTest {

    private TicketListPanel panel;
    private Object OLDEST_FIRST;
    private Object NEWEST_FIRST;

    @BeforeAll
    void setUp() throws Exception {
        System.setProperty("java.awt.headless", "true");
        panel = new TicketListPanel(() -> {}, id -> {});
        for (Class<?> cls : TicketListPanel.class.getDeclaredClasses()) {
            if (cls.isEnum() && cls.getSimpleName().equals("SortOrder")) {
                OLDEST_FIRST = cls.getEnumConstants()[0];
                NEWEST_FIRST = cls.getEnumConstants()[1];
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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

    private TicketListPanel.TicketRow row(Long id, String createdAt, int points) {
        return new TicketListPanel.TicketRow(
                id, "INC-" + id, "T" + id, "D",
                "MEDIUM", "Opened", "user", "Sin asignar",
                createdAt, points);
    }

    // =========================================================================
    // T09 — Puntos > fecha en modo OLDEST_FIRST
    // =========================================================================

    @Test @Order(9)
    @DisplayName("T09 - OLDEST_FIRST: usuario con mas puntos primero, sin importar la fecha")
    void testSort_pointsFirst_oldestMode() throws Exception {
        setSortOrder(OLDEST_FIRST);
        setFiltered(List.of(
                row(1L, "2026-01-01T00:00:00",  20),  // mas antiguo, menos puntos
                row(2L, "2026-04-19T23:59:00", 100),  // mas reciente, mas puntos
                row(3L, "2026-02-15T12:00:00",  60)   // intermedio
        ));
        callSortFiltered();
        List<TicketListPanel.TicketRow> sorted = getFiltered();

        assertEquals(100, sorted.get(0).createdByPoints,
                "100pts debe ir primero aunque sea el mas reciente");
        assertEquals( 60, sorted.get(1).createdByPoints, "60pts va segundo");
        assertEquals( 20, sorted.get(2).createdByPoints,
                "20pts va ultimo aunque sea el mas antiguo");
    }

    // =========================================================================
    // T10 — Puntos > fecha en modo NEWEST_FIRST
    // =========================================================================

    @Test @Order(10)
    @DisplayName("T10 - NEWEST_FIRST: usuario con mas puntos primero, sin importar la fecha")
    void testSort_pointsFirst_newestMode() throws Exception {
        setSortOrder(NEWEST_FIRST);
        setFiltered(List.of(
                row(1L, "2026-04-19T23:59:00", 10),   // muy reciente, pocos puntos
                row(2L, "2026-01-01T00:00:00", 100)   // muy antiguo, muchos puntos
        ));
        callSortFiltered();
        assertEquals(100, getFiltered().get(0).createdByPoints,
                "100pts debe ir primero aunque su ticket sea el mas antiguo");
    }

    // =========================================================================
    // T11 — Desempate por fecha: OLDEST_FIRST
    // =========================================================================

    @Test @Order(11)
    @DisplayName("T11 - OLDEST_FIRST: mismos puntos -> ticket mas antiguo primero")
    void testSort_samePoints_oldestFirst() throws Exception {
        setSortOrder(OLDEST_FIRST);
        setFiltered(List.of(
                row(1L, "2026-04-03T10:00:00",  80),
                row(2L, "2026-04-01T10:00:00",  80),
                row(3L, "2026-04-02T10:00:00",  80)
        ));
        callSortFiltered();
        assertTrue(getFiltered().get(0).createdAt.startsWith("2026-04-01"),
                "Apr-01 debe ir primero (mas antiguo)");
        assertTrue(getFiltered().get(2).createdAt.startsWith("2026-04-03"),
                "Apr-03 debe ir ultimo (mas reciente)");
    }

    // =========================================================================
    // T12 — Desempate por fecha: NEWEST_FIRST
    // =========================================================================

    @Test @Order(12)
    @DisplayName("T12 - NEWEST_FIRST: mismos puntos -> ticket mas reciente primero")
    void testSort_samePoints_newestFirst() throws Exception {
        setSortOrder(NEWEST_FIRST);
        setFiltered(List.of(
                row(1L, "2026-04-01T10:00:00",  80),
                row(2L, "2026-04-03T10:00:00",  80),
                row(3L, "2026-04-02T10:00:00",  80)
        ));
        callSortFiltered();
        assertTrue(getFiltered().get(0).createdAt.startsWith("2026-04-03"),
                "Apr-03 debe ir primero (mas reciente)");
        assertTrue(getFiltered().get(2).createdAt.startsWith("2026-04-01"),
                "Apr-01 debe ir ultimo (mas antiguo)");
    }

    // =========================================================================
    // T13 — Escenario complejo con mezcla de puntos y fechas
    // =========================================================================

    @Test @Order(13)
    @DisplayName("T13 - Escenario complejo: grupos por puntos, orden de fecha dentro de cada grupo")
    void testSort_complexScenario() throws Exception {
        setSortOrder(OLDEST_FIRST);
        setFiltered(List.of(
                row(1L, "2026-04-05T10:00:00",  30),
                row(2L, "2026-04-01T10:00:00",  90),
                row(3L, "2026-04-03T10:00:00",  30),
                row(4L, "2026-04-02T10:00:00",  90)
        ));
        callSortFiltered();
        List<TicketListPanel.TicketRow> s = getFiltered();

        // Los dos de 500pts van primero, ordenados por fecha asc
        assertEquals( 90, s.get(0).createdByPoints);
        assertEquals( 90, s.get(1).createdByPoints);
        assertTrue(s.get(0).createdAt.compareTo(s.get(1).createdAt) < 0,
                "Entre los de 90pts, el mas antiguo va primero");

        // Los dos de 100pts van despues, ordenados por fecha asc
        assertEquals( 30, s.get(2).createdByPoints);
        assertEquals( 30, s.get(3).createdByPoints);
        assertTrue(s.get(2).createdAt.compareTo(s.get(3).createdAt) < 0,
                "Entre los de 30pts, el mas antiguo va primero");
    }

    // =========================================================================
    // T14 — Casos extremos: lista vacia y fecha null al final
    // =========================================================================

    @Test @Order(14)
    @DisplayName("T14 - Casos extremos: lista vacia no lanza excepcion; fecha null va al final")
    void testSort_edgeCases() throws Exception {
        setSortOrder(OLDEST_FIRST);

        // Lista vacia -> sin excepcion
        setFiltered(new ArrayList<>());
        assertDoesNotThrow(this::callSortFiltered, "Lista vacia no debe lanzar excepcion");

        // Fecha null -> nullsLast: el ticket sin fecha va al final
        setFiltered(List.of(
                row(1L, null,                     50),
                row(2L, "2026-04-01T10:00:00",   50)
        ));
        callSortFiltered();
        assertNull(getFiltered().get(1).createdAt,
                "El ticket con fecha null debe ir al final (nullsLast)");
    }

    // =========================================================================
    // T15 — TicketRow: constructor e inmutabilidad de createdByPoints
    // =========================================================================

    @Test @Order(15)
    @DisplayName("T15 - TicketRow: constructor inicializa todos los campos; createdByPoints es final")
    void testTicketRow() throws Exception {
        TicketListPanel.TicketRow r = new TicketListPanel.TicketRow(
                42L, "INC-42", "Titulo", "Desc",
                "HIGH", "Opened", "user1", "agente1",
                "2026-04-19T10:00:00",  75);

        assertEquals(42L,                  r.id);
        assertEquals("INC-42",             r.ref);
        assertEquals("HIGH",               r.priority);
        assertEquals("Opened",             r.status);
        assertEquals("user1",              r.createdBy);
        assertEquals( 75,                  r.createdByPoints);
        assertEquals("2026-04-19T10:00:00",r.createdAt);

        assertTrue(
            java.lang.reflect.Modifier.isFinal(
                TicketListPanel.TicketRow.class
                    .getDeclaredField("createdByPoints").getModifiers()),
            "createdByPoints debe ser un campo final (inmutable)");
    }
}
