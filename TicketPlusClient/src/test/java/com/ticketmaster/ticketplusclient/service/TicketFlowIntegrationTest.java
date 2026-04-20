package com.ticketmaster.ticketplusclient.service;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.AuthService;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import com.ticketmaster.ticketplusclient.session.TicketService;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves d'integració del flux de tickets contra el servidor real.
 *
 * Requisits:
 * - servidor encès
 * - usuari user1 / admin123 existent
 */
@DisplayName("Flux de tickets - Proves d'integració amb servidor real")
class TicketFlowIntegrationTest {

    private static final String USERNAME = "user1";
    private static final String PASSWORD = "admin123";
    private static final int TIMEOUT_SECONDS = 20;

    private AuthService authService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() throws InterruptedException {
        authService = new AuthService();
        ticketService = new TicketService();

        SessionManager.getInstance().clearSession();
        ClientAPI.reset();

        CountDownLatch loginLatch = new CountDownLatch(1);
        AtomicReference<String> loginError = new AtomicReference<>();

        authService.login(USERNAME, PASSWORD, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(com.ticketmaster.ticketplusclient.model.LoginResponse response) {
                loginLatch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                loginError.set(errorMessage);
                loginLatch.countDown();
            }
        });

        assertTrue(loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout fent login abans de la prova");
        assertNull(loginError.get(),
                "El login previ ha de funcionar. Error: " + loginError.get());
        assertTrue(SessionManager.getInstance().isLoggedIn(),
                "La sessió ha d'estar iniciada abans de provar tickets");
    }

    @AfterEach
    void tearDown() {
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();
    }

    @Test
    @DisplayName("1) Crear ticket correctament")
    void testCreateTicket_ok() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> createdTicket = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        String uniqueTitle = "Test creacio " + UUID.randomUUID();

        ticketService.createTicket(
                uniqueTitle,
                "Descripcio prova creacio ticket",
                "HIGH",
                new TicketService.ServiceCallback<>() {
                    @Override
                    public void onSuccess(TicketDTO result) {
                        createdTicket.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        error.set(errorMessage);
                        latch.countDown();
                    }
                }
        );

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout en crear ticket");
        assertNull(error.get(),
                "No hi hauria d'haver error en crear ticket: " + error.get());
        assertNotNull(createdTicket.get(),
                "S'hauria d'haver creat el ticket");
        assertNotNull(createdTicket.get().getId(),
                "El ticket creat ha de tenir id");
        assertEquals(uniqueTitle, createdTicket.get().getTitle(),
                "El títol retornat ha de coincidir");
        assertEquals("HIGH", createdTicket.get().getPriority(),
                "La prioritat retornada ha de coincidir");
    }

    @Test
    @DisplayName("2) Llistar els meus tickets correctament")
    void testListMyTickets_ok() throws InterruptedException {
        String uniqueTitle = "Test llistat " + UUID.randomUUID();
        TicketDTO created = createTicketBlocking(uniqueTitle, "Descripcio prova llistat", "MEDIUM");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<TicketDTO>> ticketsRef = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.getTickets(new TicketService.ServiceCallback<>() {
            @Override
            public void onSuccess(List<TicketDTO> result) {
                ticketsRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                error.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout en llistar tickets");
        assertNull(error.get(),
                "No hi hauria d'haver error en llistar tickets: " + error.get());
        assertNotNull(ticketsRef.get(),
                "La llista de tickets no ha de ser null");
        assertFalse(ticketsRef.get().isEmpty(),
                "La llista de tickets no hauria d'estar buida");

        boolean found = ticketsRef.get().stream()
                .anyMatch(t -> t.getId().equals(created.getId())
                        && uniqueTitle.equals(t.getTitle()));

        assertTrue(found,
                "El ticket creat ha d'aparèixer al llistat dels meus tickets");
    }

    @Test
    @DisplayName("3) Veure el detall d'un ticket correctament")
    void testGetTicketDetail_ok() throws InterruptedException {
        String uniqueTitle = "Test detall " + UUID.randomUUID();
        TicketDTO created = createTicketBlocking(uniqueTitle, "Descripcio prova detall", "LOW");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> detailRef = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.getTicket(created.getId(), new TicketService.ServiceCallback<>() {
            @Override
            public void onSuccess(TicketDTO result) {
                detailRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                error.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout en obtenir el detall del ticket");
        assertNull(error.get(),
                "No hi hauria d'haver error en veure detall: " + error.get());
        assertNotNull(detailRef.get(),
                "El detall retornat no ha de ser null");
        assertEquals(created.getId(), detailRef.get().getId(),
                "L'id del detall ha de coincidir");
        assertEquals(uniqueTitle, detailRef.get().getTitle(),
                "El títol del detall ha de coincidir");
        assertEquals("Descripcio prova detall", detailRef.get().getDescription(),
                "La descripció del detall ha de coincidir");
        assertEquals("LOW", detailRef.get().getPriority(),
                "La prioritat del detall ha de coincidir");
    }

    private TicketDTO createTicketBlocking(String title, String description, String priority)
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TicketDTO> created = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        ticketService.createTicket(title, description, priority,
                new TicketService.ServiceCallback<>() {
                    @Override
                    public void onSuccess(TicketDTO result) {
                        created.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        error.set(errorMessage);
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Timeout en crear ticket auxiliar");
        assertNull(error.get(),
                "Error inesperat creant ticket auxiliar: " + error.get());
        assertNotNull(created.get(),
                "El ticket auxiliar s'hauria d'haver creat");

        return created.get();
    }
}