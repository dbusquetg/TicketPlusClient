package com.tickemaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.api.ClientAPI;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dialog;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserManagementPanel - creación y borrado de usuarios/admin")
class UserManagementPanelTest {

    private static final String PANEL_CLASS_NAME =
            "com.ticketmaster.ticketplusclient.gui.UserManagementPanel";

    private MockWebServer server;

    @BeforeAll
    static void swingMode() {
        System.setProperty("java.awt.headless", "false");
    }

    @AfterEach
    void tearDown() throws Exception {
        closeAllDialogs();
        SessionManager.getInstance().clearSession();
        ClientAPI.reset();

        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    @Test
    @DisplayName("T01 - Al abrir la pantalla carga usuarios activos en la tabla")
    void alAbrirCargaUsuariosActivosEnLaTabla() throws Exception {
        String userA = testUsername("user_a");
        String userB = testUsername("user_b");

        try (Fixture fx = newFixture(usersJson(userA, "ADMIN", userB, "USER"))) {
            waitForTableRows(fx.panel, 2);

            assertIntEquals(2, table(fx.panel).getRowCount(), "La tabla debe tener 2 usuarios");
            assertObjectEquals(userA, table(fx.panel).getValueAt(0, 0), "Primera fila: username");
            assertObjectEquals("ADMIN", table(fx.panel).getValueAt(0, 1), "Primera fila: rol");
            assertObjectEquals("Yes", table(fx.panel).getValueAt(0, 2), "Primera fila: activo");
            assertObjectEquals(userB, table(fx.panel).getValueAt(1, 0), "Segunda fila: username");
            assertObjectEquals("USER", table(fx.panel).getValueAt(1, 1), "Segunda fila: rol");

            RecordedRequest request = takeRequest(fx.server);
            assertObjectEquals("GET", request.getMethod(), "Debe cargar usuarios con GET");
            assertObjectEquals("/api/users", request.getPath(), "Endpoint de carga de usuarios");
        }
    }

    @Test
    @DisplayName("T02 - Crear USER envía datos, limpia formulario y recarga tabla")
    void crearUsuarioUserEnviaDatosLimpiaFormularioRecargaTabla() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String existingUser = testUsername("user_actual");
        String newUser = testUsername("nuevo_user");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", existingUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.OK_OPTION)) {

            fx.server.enqueue(jsonResponse(userJson(100L, newUser, "USER")));
            fx.server.enqueue(jsonResponse(usersJson(existingAdmin, "ADMIN", existingUser, "USER", newUser, "USER")));

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                usernameField(fx.panel).setText(newUser);
                passwordField(fx.panel).setText("pass123");
                roleCombo(fx.panel).setSelectedItem("USER");
                createButton(fx.panel).doClick();
            });

            RecordedRequest createRequest = takeRequest(fx.server);
            assertObjectEquals("POST", createRequest.getMethod(), "Crear usuario debe hacer POST");
            assertObjectEquals("/api/users", createRequest.getPath(), "Endpoint de creación");

            String body = createRequest.getBody().readUtf8();
            assertTrue(body.contains("\"username\":\"" + newUser + "\""), "Debe enviar el username de prueba");
            assertTrue(body.contains("\"password\":\"pass123\""), "Debe enviar la contraseña");
            assertTrue(body.contains("\"role\":\"USER\""), "Debe enviar rol USER");

            RecordedRequest refreshRequest = takeRequest(fx.server);
            assertObjectEquals("GET", refreshRequest.getMethod(), "Tras crear debe recargar la tabla");
            assertObjectEquals("/api/users", refreshRequest.getPath(), "Endpoint de recarga");

            waitUntil(() -> containsUsername(fx.panel, newUser), "El usuario creado debe aparecer en tabla");

            assertObjectEquals("", usernameField(fx.panel).getText(), "Username debe quedar limpio");
            assertIntEquals(0, passwordField(fx.panel).getPassword().length, "Password debe quedar limpia");
            assertObjectEquals("USER", roleCombo(fx.panel).getSelectedItem(), "El rol vuelve a USER");
        }
    }

    @Test
    @DisplayName("T03 - Crear ADMIN envía el rol ADMIN al servicio")
    void crearUsuarioAdminEnviaRolAdmin() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String existingUser = testUsername("user_actual");
        String newAdmin = testUsername("nuevo_admin");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", existingUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.OK_OPTION)) {

            fx.server.enqueue(jsonResponse(userJson(101L, newAdmin, "ADMIN")));
            fx.server.enqueue(jsonResponse(usersJson(existingAdmin, "ADMIN", existingUser, "USER", newAdmin, "ADMIN")));

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                usernameField(fx.panel).setText(newAdmin);
                passwordField(fx.panel).setText("admin123");
                roleCombo(fx.panel).setSelectedItem("ADMIN");
                createButton(fx.panel).doClick();
            });

            RecordedRequest createRequest = takeRequest(fx.server);
            assertObjectEquals("POST", createRequest.getMethod(), "Crear admin debe hacer POST");
            assertObjectEquals("/api/users", createRequest.getPath(), "Endpoint de creación");

            String body = createRequest.getBody().readUtf8();
            assertTrue(body.contains("\"username\":\"" + newAdmin + "\""), "Debe enviar el username admin de prueba");
            assertTrue(body.contains("\"role\":\"ADMIN\""), "Debe enviar rol ADMIN");

            takeRequest(fx.server);
            waitUntil(() -> containsUsername(fx.panel, newAdmin), "El ADMIN creado debe aparecer en tabla");
        }
    }

    @Test
    @DisplayName("T04 - No crea si el username está vacío")
    void noCreaSiUsernameEstaVacio() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String existingUser = testUsername("user_actual");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", existingUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.OK_OPTION)) {

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                usernameField(fx.panel).setText("   ");
                passwordField(fx.panel).setText("pass123");
                createButton(fx.panel).doClick();
            });

            Thread.sleep(150L);
            assertIntEquals(1, fx.server.getRequestCount(), "No debe hacer POST si el username está vacío");
        }
    }

    @Test
    @DisplayName("T05 - No crea si la contraseña está vacía")
    void noCreaSiPasswordEstaVacio() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String existingUser = testUsername("user_actual");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", existingUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.OK_OPTION)) {

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                usernameField(fx.panel).setText(testUsername("sin_pass"));
                passwordField(fx.panel).setText("   ");
                createButton(fx.panel).doClick();
            });

            Thread.sleep(150L);
            assertIntEquals(1, fx.server.getRequestCount(), "No debe hacer POST si la contraseña está vacía");
        }
    }

    @Test
    @DisplayName("T06 - Eliminar seleccionado confirmado llama a DELETE y recarga tabla")
    void eliminarSeleccionadoConfirmadoLlamaDeleteYRecargaTabla() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String deleteUser = testUsername("delete_user");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", deleteUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.YES_OPTION, JOptionPane.OK_OPTION)) {

            fx.server.enqueue(new MockResponse().setResponseCode(204));
            fx.server.enqueue(jsonResponse(usersJson(existingAdmin, "ADMIN")));

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                int row = findRowByUsername(fx.panel, deleteUser);
                table(fx.panel).setRowSelectionInterval(row, row);
                deleteButton(fx.panel).doClick();
            });

            RecordedRequest deleteRequest = takeRequest(fx.server);
            assertObjectEquals("DELETE", deleteRequest.getMethod(), "Borrar debe hacer DELETE");
            assertObjectEquals("/api/users/" + deleteUser, deleteRequest.getPath(), "Endpoint de borrado");

            RecordedRequest refreshRequest = takeRequest(fx.server);
            assertObjectEquals("GET", refreshRequest.getMethod(), "Tras borrar debe recargar");
            assertObjectEquals("/api/users", refreshRequest.getPath(), "Endpoint de recarga");

            waitUntil(() -> !containsUsername(fx.panel, deleteUser),
                    "El usuario de prueba borrado debe desaparecer de la tabla");

            assertFalse(containsUsername(fx.panel, deleteUser), "El usuario de prueba no debe aparecer tras borrarlo");
        }
    }

    @Test
    @DisplayName("T07 - Si se cancela la confirmación no elimina usuario")
    void cancelarEliminacionNoLlamaServicio() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String cancelUser = testUsername("cancel_delete");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", cancelUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.NO_OPTION)) {

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                int row = findRowByUsername(fx.panel, cancelUser);
                table(fx.panel).setRowSelectionInterval(row, row);
                deleteButton(fx.panel).doClick();
            });

            Thread.sleep(150L);
            assertIntEquals(1, fx.server.getRequestCount(), "No debe hacer DELETE si se cancela la confirmación");
            assertTrue(containsUsername(fx.panel, cancelUser), "El usuario debe seguir en tabla si se cancela");
        }
    }

    @Test
    @DisplayName("T08 - No permite eliminar el propio usuario autenticado")
    void noPermiteEliminarUsuarioActual() throws Exception {
        String currentUser = "admin";
        String otherUser = testUsername("otro_user");

        try (Fixture fx = newFixture(usersJson(currentUser, "ADMIN", otherUser, "USER"));
             DialogAutoCloser ignored = DialogAutoCloser.closeNext(JOptionPane.OK_OPTION)) {

            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> {
                int row = findRowByUsername(fx.panel, currentUser);
                table(fx.panel).setRowSelectionInterval(row, row);
                deleteButton(fx.panel).doClick();
            });

            Thread.sleep(150L);
            assertIntEquals(1, fx.server.getRequestCount(), "No debe hacer DELETE al intentar borrar el propio usuario");
            assertTrue(containsUsername(fx.panel, currentUser), "El usuario actual debe seguir en tabla");
        }
    }

    @Test
    @DisplayName("T09 - Botón Back ejecuta el callback de vuelta")
    void botonBackEjecutaCallback() throws Exception {
        String existingAdmin = testUsername("admin_actual");
        String existingUser = testUsername("user_actual");

        try (Fixture fx = newFixture(usersJson(existingAdmin, "ADMIN", existingUser, "USER"))) {
            waitForTableRows(fx.panel, 2);
            takeRequest(fx.server);

            onEdt(() -> backButton(fx.panel).doClick());

            assertIntEquals(1, fx.backCalls.get(), "El callback de Back debe ejecutarse una vez");
        }
    }

    private Fixture newFixture(String initialUsersJson) throws Exception {
        server = new MockWebServer();
        server.enqueue(jsonResponse(initialUsersJson));
        server.start();

        injectRetrofit(server);
        SessionManager.getInstance().startSession("token-test", "ADMIN", "admin");

        AtomicInteger backCalls = new AtomicInteger();
        AtomicReference<Object> panelRef = new AtomicReference<>();

        onEdt(() -> panelRef.set(createPanelInstance(backCalls::incrementAndGet)));

        return new Fixture(panelRef.get(), server, backCalls);
    }

    private static Object createPanelInstance(Runnable onBack) throws Exception {
        Class<?> panelClass = Class.forName(PANEL_CLASS_NAME);
        Constructor<?> constructor = panelClass.getConstructor(Runnable.class);
        return constructor.newInstance(onBack);
    }

    private static void injectRetrofit(MockWebServer server) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body());

                    String token = SessionManager.getInstance().getToken();
                    if (token != null) {
                        builder.header("Authorization", "Bearer " + token);
                    }

                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Field retrofitField = ClientAPI.class.getDeclaredField("retrofit");
        retrofitField.setAccessible(true);
        retrofitField.set(null, retrofit);
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String testUsername(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "joc_" + prefix + "_" + suffix;
    }

    private static String usersJson(String username1, String role1) {
        return "[" + userJson(1L, username1, role1) + "]";
    }

    private static String usersJson(String username1, String role1, String username2, String role2) {
        return "[" + userJson(1L, username1, role1) + "," + userJson(2L, username2, role2) + "]";
    }

    private static String usersJson(String username1, String role1,
                                    String username2, String role2,
                                    String username3, String role3) {
        return "[" + userJson(1L, username1, role1) + ","
                + userJson(2L, username2, role2) + ","
                + userJson(3L, username3, role3) + "]";
    }

    private static String userJson(long id, String username, String role) {
        return "{"
                + "\"id\":" + id + ","
                + "\"username\":\"" + username + "\","
                + "\"role\":\"" + role + "\","
                + "\"active\":true"
                + "}";
    }

    private static RecordedRequest takeRequest(MockWebServer server) throws Exception {
        RecordedRequest request = server.takeRequest(3L, TimeUnit.SECONDS);
        assertNotNull(request, "No ha llegado la petición HTTP esperada al MockWebServer");
        return request;
    }

    private static void waitForTableRows(Object panel, int expectedRows) throws Exception {
        waitUntil(() -> table(panel).getRowCount() == expectedRows,
                "La tabla no ha llegado a tener " + expectedRows + " filas");
    }

    private static void waitUntil(ThrowingBooleanSupplier condition, String errorMessage) throws Exception {
        long deadline = System.currentTimeMillis() + 3000L;

        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25L);
        }

        throw new AssertionError(errorMessage);
    }

    private static void assertIntEquals(int expected, int actual, String message) {
        assertTrue(actual == expected, message + " | esperado=" + expected + ", actual=" + actual);
    }

    private static void assertObjectEquals(Object expected, Object actual, String message) {
        if (expected == null) {
            assertTrue(actual == null, message + " | esperado=null, actual=" + actual);
        } else {
            assertTrue(expected.equals(actual), message + " | esperado=" + expected + ", actual=" + actual);
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Object panel;
        private final MockWebServer server;
        private final AtomicInteger backCalls;

        private Fixture(Object panel, MockWebServer server, AtomicInteger backCalls) {
            this.panel = panel;
            this.server = server;
            this.backCalls = backCalls;
        }

        @Override
        public void close() {
            closeAllDialogs();
        }
    }

    private static final class DialogAutoCloser implements AutoCloseable {
        private final Queue<Integer> responses = new ArrayDeque<>();
        private final Set<Window> handledWindows =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        private final Timer timer;

        private DialogAutoCloser(int... responses) {
            for (int response : responses) {
                this.responses.add(response);
            }

            this.timer = new Timer(40, e -> closePendingDialogs());
            this.timer.setInitialDelay(0);
            this.timer.setRepeats(true);
            this.timer.start();
        }

        static DialogAutoCloser closeNext(int... responses) {
            return new DialogAutoCloser(responses);
        }

        private void closePendingDialogs() {
            if (responses.isEmpty()) {
                timer.stop();
                return;
            }

            for (Window window : Window.getWindows()) {
                if (!(window instanceof Dialog) || !window.isShowing() || handledWindows.contains(window)) {
                    continue;
                }

                JOptionPane optionPane = findOptionPane(window);
                if (optionPane == null || responses.isEmpty()) {
                    continue;
                }

                handledWindows.add(window);
                optionPane.setValue(responses.remove());
                window.setVisible(false);
                window.dispose();

                if (responses.isEmpty()) {
                    timer.stop();
                    return;
                }
            }
        }

        @Override
        public void close() {
            timer.stop();
            closeAllDialogs();
        }
    }

    private static JOptionPane findOptionPane(java.awt.Container container) {
        if (container instanceof JOptionPane) {
            return (JOptionPane) container;
        }

        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof java.awt.Container) {
                JOptionPane found = findOptionPane((java.awt.Container) component);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static void closeAllDialogs() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Dialog && window.isShowing()) {
                window.setVisible(false);
                window.dispose();
            }
        }
    }

    private static JTable table(Object panel) throws Exception {
        return field(panel, "userTable", JTable.class);
    }

    private static JTextField usernameField(Object panel) throws Exception {
        return field(panel, "usernameField", JTextField.class);
    }

    private static JPasswordField passwordField(Object panel) throws Exception {
        return field(panel, "passwordField", JPasswordField.class);
    }

    private static JComboBox<?> roleCombo(Object panel) throws Exception {
        return field(panel, "roleCombo", JComboBox.class);
    }

    private static JButton createButton(Object panel) throws Exception {
        return field(panel, "createBtn", JButton.class);
    }

    private static JButton deleteButton(Object panel) throws Exception {
        return field(panel, "deleteBtn", JButton.class);
    }

    private static JButton backButton(Object panel) throws Exception {
        return field(panel, "backBtn", JButton.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(target);
        assertNotNull(value, "El campo '" + name + "' no debería ser null");
        return type.cast(value);
    }

    private static int findRowByUsername(Object panel, String username) throws Exception {
        JTable table = table(panel);
        for (int row = 0; row < table.getRowCount(); row++) {
            if (username.equals(table.getValueAt(row, 0))) {
                return row;
            }
        }
        throw new AssertionError("No se encontró el usuario en tabla: " + username);
    }

    private static boolean containsUsername(Object panel, String username) throws Exception {
        JTable table = table(panel);
        for (int row = 0; row < table.getRowCount(); row++) {
            if (username.equals(table.getValueAt(row, 0))) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }

    private static void onEdt(ThrowingRunnable task) throws Exception {
        onEdt(() -> {
            task.run();
            return null;
        });
    }

    private static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable t) {
                error.set(t);
            }
        });

        if (error.get() != null) {
            if (error.get() instanceof Exception) {
                throw (Exception) error.get();
            }
            if (error.get() instanceof Error) {
                throw (Error) error.get();
            }
            throw new RuntimeException(error.get());
        }

        return result.get();
    }
}