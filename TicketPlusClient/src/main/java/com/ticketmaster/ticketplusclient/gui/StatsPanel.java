package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.CommentDTO;
import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import com.ticketmaster.ticketplusclient.session.TicketService;
import java.awt.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Objects;

/**
 * Panel de estadísticas y métricas de tickets.
 *
 * <p>Modos:</p>
 * <ul>
 *   <li><b>Agente</b> ({@code userMode=false}): todos los tickets, sidebar de filtros visible.</li>
 *   <li><b>Usuario</b> ({@code userMode=true}): solo sus incidencias, sidebar oculta.</li>
 * </ul>
 *
 * @author Christian
 * @see DashboardAgentGUI
 * @see DashboardUserGUI
 */
public class StatsPanel extends JPanel {

    // ─── Colores ──────────────────────────────────────────────────────────────

    private static final Color BG_DARK      = new Color(21,  25,  28);
    private static final Color BG_MID       = new Color(34,  40,  44);
    private static final Color BG_CARD      = new Color(248, 249, 250);
    private static final Color ACCENT_BLUE  = new Color(54,  81,  207);
    private static final Color TEXT_WHITE   = Color.WHITE;
    private static final Color TEXT_MUTED   = new Color(160, 160, 160);
    private static final Color TEXT_DARK    = new Color(30,  30,  30);

    // Colores de estado (barra oscura)
    private static final Color COL_OPEN     = new Color(54,  130, 207);
    private static final Color COL_PENDING  = new Color(220, 160, 40);
    private static final Color COL_PROGRESS = new Color(130, 80,  200);
    private static final Color COL_SOLVED   = new Color(60,  180, 100);
    private static final Color COL_CLOSED   = new Color(34,  139, 34);

    // Colores de prioridad (gráfico dona)
    private static final Color COL_HIGH     = new Color(220, 60,  60);
    private static final Color COL_MEDIUM   = new Color(220, 160, 40);
    private static final Color COL_LOW      = new Color(60,  180, 80);
    private static final Color COL_CRITICAL = new Color(150, 20,  20);

    // ─── Modo de filtro ───────────────────────────────────────────────────────

    /** Modos de filtro disponibles en modo agente. */
    private enum FilterMode {
        /** Todos los tickets del sistema. */
        ALL,
        /** Tickets asignados al agente autenticado. */
        MY_TICKETS,
        /** Tickets asignados al agente seleccionado en el combo. */
        BY_AGENT
    }

    // ─── Configuración ────────────────────────────────────────────────────────

    /**
     * {@code true} si el panel opera en modo usuario.
     * En modo usuario se filtra por {@code createdBy} y se oculta la sidebar.
     */
    private final boolean userMode;

    /** Servicio de tickets. */
    private final TicketService ticketService;

    /** Acción ejecutada al pulsar Back. */
    private final Runnable onBack;

    // ─── Estado interno ───────────────────────────────────────────────────────

    /** Lista completa de tickets cargados del servidor. */
    private final List<TicketDTO> allTickets     = new ArrayList<>();

    /** Lista filtrada según el modo y filtro activos. */
    private List<TicketDTO>       filteredTickets = new ArrayList<>();

    /** Modo de filtro activo (solo en modo agente). */
    private FilterMode filterMode    = FilterMode.ALL;

    /** Agente seleccionado para el modo BY_AGENT. */
    private String     selectedAgent = null;

    // ─── Referencias UI — barra oscura superior (igual que TicketListPanel) ──

    /** Contenedor de la barra oscura de stats. Su altura es animable. */
    private JPanel darkStatsPanel;

    // ─── Referencias UI — tarjetas de métricas en el panel blanco ────────────

    /** Total de tickets abiertos durante el último mes. */
    private JLabel lblOpenedMonth;

    /** Total de comentarios del último mes. */
    private JLabel lblComments;

    /** Tickets activos con más de 4h sin resolver. */
    private JLabel lblOverdue;

    /** Tiempo promedio de resolución. */
    private JLabel lblAvgTime;

    // ─── Referencias UI — gráficos ────────────────────────────────────────────

    /** Panel del gráfico de dona (prioridad). */
    private JPanel donutPanel;

    /** Panel del gráfico de barras (estado). */
    private JPanel barPanel;

    /** Panel del gráfico de línea (últimos 7 días). */
    private JPanel linePanel;

    // ─── Referencias UI — sidebar (solo modo agente) ──────────────────────────

    /** Combo de agentes para el filtro BY_AGENT. */
    private JComboBox<String> agentCombo;

    /** Contenedor del combo, se muestra/oculta según el filtro activo. */
    private JPanel agentComboWrapper;

    /** Botones de filtro indexados por modo. */
    private final Map<FilterMode, JButton> filterButtons = new EnumMap<>(FilterMode.class);

    // ─── Constructor ──────────────────────────────────────────────────────────

    /**
     * Crea el panel de estadísticas.
     *
     * @param onBack   acción ejecutada al pulsar Back
     * @param userMode {@code true} para modo usuario — filtra por incidencias
     *                 propias y oculta la barra lateral de filtros
     */
    public StatsPanel(Runnable onBack, boolean userMode) {
        this.onBack        = onBack;
        this.userMode      = userMode;
        this.ticketService = new TicketService();

        setLayout(new BorderLayout(0, 0));
        setBackground(BG_MID);

        add(buildDarkStatsBar(), BorderLayout.NORTH);
        add(buildMainArea(),     BorderLayout.CENTER);
        add(buildBottomBar(),    BorderLayout.SOUTH);
    }

    // ─── Barra oscura superior ────────────────────────────────────────────────

    /**
     * Construye la barra oscura superior con las 4 tarjetas de estado,
     * idéntica visualmente a la barra de estadísticas de {@link TicketListPanel}.
     *
     * <p>Tarjetas: Open Tickets, In Progress, Answer Pending, Solved.</p>
     *
     * @return panel con las cuatro tarjetas de estado
     */
    private JPanel buildDarkStatsBar() {
        darkStatsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        darkStatsPanel.setBackground(BG_MID);
        darkStatsPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        darkStatsPanel.setPreferredSize(new Dimension(0, 100));

        // Las etiquetas se rellenan en refreshDarkStats() tras cargar datos
        darkStatsPanel.add(buildDarkStatCard("Open Tickets",    "—", new Color(207, 97,  54), "darkOpen"));
        darkStatsPanel.add(buildDarkStatCard("In Progress",     "—", new Color(54,  130, 207), "darkProgress"));
        darkStatsPanel.add(buildDarkStatCard("Answer Pending",  "—", new Color(207, 160, 54), "darkPending"));
        darkStatsPanel.add(buildDarkStatCard("Solved",          "—", new Color(54,  160, 100), "darkSolved"));

        return darkStatsPanel;
    }

    /**
     * Construye una tarjeta individual de la barra oscura superior.
     *
     * @param title  título de la tarjeta
     * @param value  valor inicial a mostrar
     * @param accent color del borde izquierdo de acento
     * @param name   nombre para identificar el componente y actualizarlo
     * @return panel con la tarjeta oscura
     */
    private JPanel buildDarkStatCard(String title, String value,
                                     Color accent, String name) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setName(name);
        card.setBackground(BG_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setFont(titleLbl.getFont().deriveFont(11f));
        titleLbl.setName(name + "_title");

        JLabel valueLbl = new JLabel(value);
        valueLbl.setForeground(TEXT_WHITE);
        valueLbl.setFont(valueLbl.getFont().deriveFont(Font.BOLD, 26f));
        valueLbl.setName(name + "_value");

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        return card;
    }

    /**
     * Actualiza los valores de la barra oscura superior con los datos actuales
     * de {@link #filteredTickets}.
     */
    private void refreshDarkStats() {
        long open     = countByStatus("Opened");
        long progress = countByStatus("In Progress");
        long pending  = countByStatus("Pending");
        long solved   = countByStatus("Solved");

        updateDarkCard("darkOpen",     open);
        updateDarkCard("darkProgress", progress);
        updateDarkCard("darkPending",  pending);
        updateDarkCard("darkSolved",   solved);
    }

    /**
     * Busca la tarjeta con el nombre dado en la barra oscura y actualiza
     * la etiqueta de valor.
     *
     * @param cardName nombre del panel de la tarjeta
     * @param value    nuevo valor a mostrar
     */
    private void updateDarkCard(String cardName, long value) {
        for (Component c : darkStatsPanel.getComponents()) {
            if (!(c instanceof JPanel card) || !cardName.equals(card.getName())) continue;
            for (Component child : card.getComponents()) {
                if (child instanceof JLabel lbl
                        && (cardName + "_value").equals(lbl.getName())) {
                    lbl.setText(String.valueOf(value));
                    break;
                }
            }
        }
    }

    // ─── Área principal (panel blanco + sidebar) ──────────────────────────────

    /**
     * Construye el área principal: panel blanco central + sidebar opcional.
     *
     * @return panel con layout BorderLayout
     */
    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout(8, 0));
        area.setBackground(BG_MID);
        area.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        area.add(buildCentralCard(), BorderLayout.CENTER);

        if (!userMode) {
            area.add(buildSidebar(), BorderLayout.EAST);
        }

        return area;
    }

    /**
     * Construye el panel blanco central con las 4 tarjetas de métricas detalladas
     * en la zona superior y los 3 gráficos debajo.
     *
     * @return panel blanco con métricas y gráficos
     */
    private JPanel buildCentralCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 205), 1),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        card.add(buildWhiteMetricsRow(), BorderLayout.NORTH);
        card.add(buildChartsRow(),       BorderLayout.CENTER);

        return card;
    }

    // ─── Tarjetas de métricas del panel blanco ────────────────────────────────

    /**
     * Construye la fila de 4 tarjetas de métricas detalladas dentro del panel blanco.
     *
     * @return panel con las cuatro tarjetas
     */
    private JPanel buildWhiteMetricsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setBackground(BG_CARD);
        row.setPreferredSize(new Dimension(0, 88));

        lblOpenedMonth = new JLabel("—");
        lblComments    = new JLabel("—");
        lblOverdue     = new JLabel("—");
        lblAvgTime     = new JLabel("N/A");

        String openTitle    = userMode ? "My tickets (last month)" : "Opened (last month)";
        String overdueTitle = userMode ? "My unresolved > 4h"      : "Unresolved > 4h";

        row.add(buildWhiteMetricCard(openTitle,               lblOpenedMonth, new Color(207, 97,  54)));
        row.add(buildWhiteMetricCard("Comments (last month)", lblComments,    new Color(54,  130, 207)));
        row.add(buildWhiteMetricCard(overdueTitle,            lblOverdue,     new Color(207, 160, 54)));
        row.add(buildWhiteMetricCard("Avg. resolution time",  lblAvgTime,     new Color(54,  160, 100)));

        return row;
    }

    /**
     * Construye una tarjeta de métrica con estilo claro para el panel blanco.
     *
     * @param title  título de la métrica
     * @param label  etiqueta del valor
     * @param accent color del borde superior de acento
     * @return tarjeta de métrica sobre fondo blanco
     */
    private JPanel buildWhiteMetricCard(String title, JLabel label, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(3, 0, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            )
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(new Color(100, 100, 100));
        titleLbl.setFont(titleLbl.getFont().deriveFont(11f));

        label.setForeground(TEXT_DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(label,    BorderLayout.CENTER);
        return card;
    }

    // ─── Gráficos ─────────────────────────────────────────────────────────────

    /**
     * Construye la fila con los tres gráficos.
     *
     * @return panel con dona, barras y línea
     */
    private JPanel buildChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 1, 0));
        row.setBackground(new Color(210, 210, 215));

        row.add(buildChartSection("Total tickets by Priority",    this::buildDonutCanvas));
        row.add(buildChartSection("Total tickets by Status",      this::buildBarCanvas));
        row.add(buildChartSection("Tickets created — Last 7 days", this::buildLineCanvas));

        return row;
    }

    /**
     * Envuelve un panel de gráfico con su título.
     *
     * @param title         título del gráfico
     * @param canvasFactory proveedor del panel de dibujo
     * @return sección con título y canvas
     */
    private JPanel buildChartSection(String title,
                                     java.util.function.Supplier<JPanel> canvasFactory) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(BG_CARD);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.PLAIN, 11f));
        titleLbl.setForeground(new Color(80, 80, 80));
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        wrapper.add(titleLbl, BorderLayout.NORTH);
        wrapper.add(canvasFactory.get(), BorderLayout.CENTER);
        return wrapper;
    }

    /** Crea el canvas del gráfico de dona. */
    private JPanel buildDonutCanvas() {
        donutPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintDonut((Graphics2D) g, getWidth(), getHeight());
            }
        };
        donutPanel.setBackground(BG_CARD);
        return donutPanel;
    }

    /** Crea el canvas del gráfico de barras. */
    private JPanel buildBarCanvas() {
        barPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintBars((Graphics2D) g, getWidth(), getHeight());
            }
        };
        barPanel.setBackground(BG_CARD);
        return barPanel;
    }

    /** Crea el canvas del gráfico de línea. */
    private JPanel buildLineCanvas() {
        linePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintLine((Graphics2D) g, getWidth(), getHeight());
            }
        };
        linePanel.setBackground(BG_CARD);
        return linePanel;
    }

    // ─── Sidebar (solo modo agente) ───────────────────────────────────────────

    /**
     * Construye la barra lateral de filtros.
     * Solo se añade al layout en modo agente.
     *
     * @return panel lateral con los botones de filtro y combo de agentes
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_DARK);
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        sidebar.setPreferredSize(new Dimension(155, 0));

        JLabel filterLabel = new JLabel("Filter by:");
        filterLabel.setForeground(TEXT_MUTED);
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD, 11f));
        filterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(filterLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        sidebar.add(buildFilterButton("All tickets", FilterMode.ALL));
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(buildFilterButton("My tickets",  FilterMode.MY_TICKETS));
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(buildFilterButton("By agent",    FilterMode.BY_AGENT));
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));

        agentComboWrapper = new JPanel(new BorderLayout());
        agentComboWrapper.setBackground(BG_DARK);
        agentComboWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        agentComboWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        agentComboWrapper.setVisible(false);

        agentCombo = new JComboBox<>();
        agentCombo.setBackground(new Color(50, 57, 64));
        agentCombo.setForeground(TEXT_WHITE);
        agentCombo.setFocusable(false);
        agentCombo.addActionListener(e -> {
            if (agentCombo.getSelectedItem() != null) {
                selectedAgent = agentCombo.getSelectedItem().toString();
                applyFilter();
            }
        });
        agentComboWrapper.add(agentCombo, BorderLayout.CENTER);
        sidebar.add(agentComboWrapper);
        sidebar.add(Box.createVerticalGlue());

        styleFilterButton(FilterMode.ALL);
        return sidebar;
    }

    /**
     * Construye un botón pill de filtro para la sidebar.
     *
     * @param label texto del botón
     * @param mode  modo de filtro que activa
     * @return botón configurado y registrado en {@link #filterButtons}
     */
    private JButton buildFilterButton(String label, FilterMode mode) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 70, 80), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setBackground(BG_DARK);
        btn.setForeground(TEXT_MUTED);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            filterMode = mode;
            styleFilterButton(mode);
            agentComboWrapper.setVisible(mode == FilterMode.BY_AGENT);
            agentComboWrapper.getParent().revalidate();
            if (mode == FilterMode.BY_AGENT && agentCombo.getItemCount() > 0) {
                selectedAgent = agentCombo.getSelectedItem().toString();
            }
            applyFilter();
        });

        filterButtons.put(mode, btn);
        return btn;
    }

    /**
     * Actualiza el estilo visual de los botones de filtro.
     *
     * @param activeMode modo activo que debe aparecer resaltado
     */
    private void styleFilterButton(FilterMode activeMode) {
        filterButtons.forEach((mode, btn) -> {
            btn.setBackground(mode == activeMode ? ACCENT_BLUE : BG_DARK);
            btn.setForeground(mode == activeMode ? TEXT_WHITE  : TEXT_MUTED);
        });
    }

    // ─── Barra inferior ───────────────────────────────────────────────────────

    /**
     * Construye la barra inferior con el botón Back.
     *
     * @return panel con el botón Back
     */
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(BG_MID);

        JButton backBtn = new JButton("Back");
        backBtn.setBackground(new Color(70, 80, 90));
        backBtn.setForeground(TEXT_WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onBack.run());

        bar.add(backBtn);
        return bar;
    }

    // ─── Carga y filtrado ─────────────────────────────────────────────────────

    /**
     * Recarga los tickets desde el servidor y actualiza toda la UI.
     * Puede llamarse en cualquier momento.
     */
    public void refresh() {
        lblOpenedMonth.setText("...");
        lblComments.setText("...");
        lblOverdue.setText("...");
        lblAvgTime.setText("...");

        ticketService.getTickets(new TicketService.ServiceCallback<List<TicketDTO>>() {
            @Override
            public void onSuccess(List<TicketDTO> tickets) {
                allTickets.clear();
                allTickets.addAll(tickets);
                populateAgentCombo();
                applyFilter();
            }

            @Override
            public void onError(String errorMessage) {
                lblOpenedMonth.setText("—");
                lblComments.setText("—");
                lblOverdue.setText("—");
                lblAvgTime.setText("—");
                System.err.println("StatsPanel — error: " + errorMessage);
            }
        });
    }

    /**
     * Rellena el combo de agentes con los valores únicos de {@link #allTickets}.
     */
    private void populateAgentCombo() {
        if (agentCombo == null) return;

        List<String> agents = allTickets.stream()
                .map(TicketDTO::getAgent)
                .filter(a -> a != null && !a.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        agentCombo.removeAllItems();
        agents.forEach(agentCombo::addItem);

        if (!agents.isEmpty() && selectedAgent == null) {
            selectedAgent = agents.get(0);
        }
    }

    /**
     * Aplica el filtro activo sobre {@link #allTickets} y actualiza la UI.
     */
    private void applyFilter() {
        String me = SessionManager.getInstance().getUsername();

        if (userMode) {
            filteredTickets = allTickets.stream()
                    .filter(t -> me.equals(t.getCreatedBy()))
                    .collect(Collectors.toList());
        } else {
            filteredTickets = switch (filterMode) {
                case MY_TICKETS -> allTickets.stream()
                        .filter(t -> me.equals(t.getAgent()))
                        .collect(Collectors.toList());
                case BY_AGENT   -> allTickets.stream()
                        .filter(t -> selectedAgent != null
                                  && selectedAgent.equals(t.getAgent()))
                        .collect(Collectors.toList());
                default         -> new ArrayList<>(allTickets);
            };
        }

        refreshDarkStats();
        updateWhiteMetrics();
        loadCommentsCount();
        repaintCharts();
    }

    /**
     * Actualiza las etiquetas del panel blanco con las métricas detalladas.
     */
    private void updateWhiteMetrics() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);

        // Tickets creados en el último mes (cualquier estado)
        long openedMonth = filteredTickets.stream()
                .filter(t -> {
                    LocalDateTime dt = parseDateTime(t.getCreatedAt());
                    return dt != null && dt.isAfter(oneMonthAgo);
                })
                .count();

        // Tickets activos con más de 4h sin resolver
        long overdueCount = filteredTickets.stream()
                .filter(t -> isActiveStatus(t.getStatus())
                          && isOlderThan4h(t.getCreatedAt()))
                .count();

        lblOpenedMonth.setText(String.valueOf(openedMonth));
        lblOverdue.setText(String.valueOf(overdueCount));
        
        double avgHours = calcAvgResolutionHours();
        lblAvgTime.setText(avgHours < 0 ? "N/A" : formatAvgTime(avgHours));
    }
    
    /**
    * Calcula el tiempo promedio de resolución en horas para los tickets
    * en estado Solved o Closed que tengan createdAt y resolvedAt informados.
    *
    * @return promedio en horas o {@code -1} si no hay datos suficientes
    */
    private double calcAvgResolutionHours() {
        List<Double> times = filteredTickets.stream()
            .filter(t -> ("Solved".equals(t.getStatus()) || "Closed".equals(t.getStatus()))
                      && t.getCreatedAt()  != null
                      && t.getResolvedAt() != null)
            .map(t -> {
                LocalDateTime created  = parseDateTime(t.getCreatedAt());
                LocalDateTime resolved = parseDateTime(t.getResolvedAt());
                if (created == null || resolved == null) return null;
                return (double) ChronoUnit.MINUTES.between(created, resolved) / 60.0;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return times.isEmpty()
            ? -1
            : times.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
    }

    /**
     * Formatea el tiempo promedio en horas a una cadena legible.
     *
     * @param hours horas promedio
     * @return cadena formateada (p.ej. "2h 30m")
     */
    private String formatAvgTime(double hours) {
        int h = (int) hours;
        int m = (int) ((hours - h) * 60);
        return h + "h " + m + "m";
    }

    /**
     * Carga el número de comentarios del último mes para cada ticket filtrado.
     * Realiza una llamada a {@link TicketService#getComments} por ticket y
     * acumula el total usando {@link AtomicInteger}.
     */
    private void loadCommentsCount() {
        if (filteredTickets.isEmpty()) {
            lblComments.setText("0");
            return;
        }

        lblComments.setText("...");
        LocalDateTime threshold  = LocalDateTime.now().minusDays(30);
        AtomicInteger total      = new AtomicInteger(0);
        AtomicInteger pending    = new AtomicInteger(filteredTickets.size());

        for (TicketDTO ticket : filteredTickets) {
            ticketService.getComments(ticket.getId(),
                new TicketService.ServiceCallback<List<CommentDTO>>() {
                    @Override
                    public void onSuccess(List<CommentDTO> comments) {
                        long recent = comments.stream()
                                .filter(c -> {
                                    LocalDateTime dt = parseDateTime(c.getCreatedAt());
                                    return dt != null && dt.isAfter(threshold);
                                })
                                .count();
                        total.addAndGet((int) recent);
                        if (pending.decrementAndGet() == 0) {
                            lblComments.setText(String.valueOf(total.get()));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (pending.decrementAndGet() == 0) {
                            lblComments.setText(String.valueOf(total.get()));
                        }
                    }
                });
        }
    }

    /**
     * Fuerza el redibujado de los tres gráficos.
     */
    private void repaintCharts() {
        if (donutPanel != null) donutPanel.repaint();
        if (barPanel   != null) barPanel.repaint();
        if (linePanel  != null) linePanel.repaint();
    }

    // ─── Helpers de datos ─────────────────────────────────────────────────────

    /**
     * {@code true} si el estado indica que el ticket está activo.
     *
     * @param status estado del ticket
     * @return {@code true} si activo
     */
    private boolean isActiveStatus(String status) {
        return "Opened".equals(status)
            || "Pending".equals(status)
            || "In Progress".equals(status);
    }

    /**
     * {@code true} si han pasado más de 4 horas desde la apertura.
     *
     * @param createdAt cadena ISO de la fecha de creación
     * @return {@code true} si el ticket tiene más de 4h abierto
     */
    private boolean isOlderThan4h(String createdAt) {
        LocalDateTime dt = parseDateTime(createdAt);
        if (dt == null) return false;
        return ChronoUnit.HOURS.between(dt, LocalDateTime.now()) >= 4;
    }

    /**
     * Intenta parsear la cadena de fecha en los formatos más habituales.
     *
     * @param raw cadena de fecha del servidor
     * @return {@link LocalDateTime} o {@code null} si falla el parseo
     */
    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (String pattern : new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss"}) {
            try {
                return LocalDateTime.parse(raw,
                        DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /**
     * Cuenta los tickets de la lista filtrada con el estado dado.
     *
     * @param status estado a contar
     * @return número de tickets
     */
    private long countByStatus(String status) {
        return filteredTickets.stream()
                .filter(t -> status.equals(t.getStatus()))
                .count();
    }

    /**
     * Cuenta los tickets de la lista filtrada con la prioridad dada.
     *
     * @param priority prioridad a contar
     * @return número de tickets
     */
    private long countByPriority(String priority) {
        return filteredTickets.stream()
                .filter(t -> priority.equals(t.getPriority()))
                .count();
    }

    /**
     * Agrupa los tickets por día de creación para los últimos 7 días.
     *
     * @return mapa ordenado día (EEE en inglés) → cantidad
     */
    private LinkedHashMap<String, Long> ticketsByDay() {
        LocalDateTime now = LocalDateTime.now();
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

        for (int i = 6; i >= 0; i--) {
            result.put(now.minusDays(i).format(dayFmt), 0L);
        }
        for (TicketDTO t : filteredTickets) {
            LocalDateTime dt = parseDateTime(t.getCreatedAt());
            if (dt == null) continue;
            long daysAgo = ChronoUnit.DAYS.between(
                    dt.toLocalDate(), now.toLocalDate());
            if (daysAgo >= 0 && daysAgo <= 6) {
                result.merge(dt.format(dayFmt), 1L, Long::sum);
            }
        }
        return result;
    }

    // ─── Pintado de gráficos ──────────────────────────────────────────────────

    /**
     * Pinta el gráfico de dona con distribución por prioridad.
     *
     * @param g2 contexto gráfico
     * @param w  ancho disponible
     * @param h  altura disponible
     */
    private void paintDonut(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        String[] priorities = {"HIGH", "MEDIUM", "LOW", "CRITICAL"};
        Color[]  colors     = {COL_HIGH, COL_MEDIUM, COL_LOW, COL_CRITICAL};
        long[]   counts     = new long[priorities.length];
        long     total      = 0;

        for (int i = 0; i < priorities.length; i++) {
            counts[i] = countByPriority(priorities[i]);
            total    += counts[i];
        }

        int legendH = 52;
        int margin  = 16;
        int diam    = Math.min(w - margin * 2, h - legendH - margin * 2);
        if (diam < 20) return;

        int cx = w / 2, cy = margin + diam / 2, hole = diam / 3;
        double start = -90.0;

        for (int i = 0; i < priorities.length; i++) {
            if (counts[i] == 0) continue;
            double sweep = total > 0 ? counts[i] * 360.0 / total : 0;
            g2.setColor(colors[i]);
            g2.fill(new Arc2D.Double(cx - diam / 2.0, cy - diam / 2.0,
                    diam, diam, start, sweep, Arc2D.PIE));
            start += sweep;
        }

        g2.setColor(BG_CARD);
        g2.fillOval(cx - hole / 2, cy - hole / 2, hole, hole);

        g2.setColor(TEXT_DARK);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g2.getFontMetrics();
        String totalStr = String.valueOf(total);
        g2.drawString(totalStr,
                cx - fm.stringWidth(totalStr) / 2, cy + fm.getAscent() / 3);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(120, 120, 120));
        g2.drawString("Total", cx - fm.stringWidth("Total") / 2,
                cy + fm.getAscent() + 6);

        int lx = margin, ly = h - legendH + 8;
        g2.setFont(g2.getFont().deriveFont(9f));
        for (int i = 0; i < priorities.length; i++) {
            if (counts[i] == 0) continue;
            g2.setColor(colors[i]);
            g2.fillOval(lx, ly, 8, 8);
            g2.setColor(TEXT_DARK);
            fm = g2.getFontMetrics();
            String txt = priorities[i] + " — " + counts[i];
            g2.drawString(txt, lx + 12, ly + 8);
            lx += fm.stringWidth(txt) + 18;
            if (lx > w - 70) { lx = margin; ly += 15; }
        }
    }

    /**
     * Pinta el gráfico de barras verticales con distribución por estado.
     *
     * @param g2 contexto gráfico
     * @param w  ancho disponible
     * @param h  altura disponible
     */
    private void paintBars(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        String[] statuses = {"Opened", "Pending", "In Progress", "Solved", "Closed"};
        Color[]  colors   = {COL_OPEN, COL_PENDING, COL_PROGRESS, COL_SOLVED, COL_CLOSED};
        long[]   counts   = new long[statuses.length];
        long     total    = 0;

        for (int i = 0; i < statuses.length; i++) {
            counts[i] = countByStatus(statuses[i]);
            total    += counts[i];
        }

        int mL = 28, mB = 36, mT = 16, mR = 8;
        int cW = w - mL - mR, cH = h - mB - mT;
        if (cW < 10 || cH < 10) return;

        long maxVal = Arrays.stream(counts).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        g2.setFont(g2.getFont().deriveFont(9f));
        for (int i = 0; i <= 4; i++) {
            int y = mT + cH - (int)(cH * i / 4.0);
            g2.setColor(new Color(210, 210, 210));
            g2.drawLine(mL, y, mL + cW, y);
            g2.setColor(new Color(130, 130, 130));
            g2.drawString(String.valueOf(maxVal * i / 4), 2, y + 4);
        }

        int barW = cW / (statuses.length * 2);
        int gapW = barW;
        int sX   = mL + gapW;

        for (int i = 0; i < statuses.length; i++) {
            int bH = (int)(cH * counts[i] / (double) maxVal);
            int x  = sX + i * (barW + gapW);
            int y  = mT + cH - bH;

            g2.setColor(new Color(colors[i].getRed(),
                    colors[i].getGreen(), colors[i].getBlue(), 55));
            g2.fillRoundRect(x + 2, y + 2, barW, bH, 4, 4);
            g2.setColor(colors[i]);
            g2.fillRoundRect(x, y, barW, bH, 4, 4);

            if (bH > 18 && total > 0) {
                String pct = (int)(counts[i] * 100 / total) + "%";
                g2.setColor(TEXT_WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 8f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pct, x + (barW - fm.stringWidth(pct)) / 2, y + bH - 5);
            }

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(TEXT_DARK);
            String cnt = String.valueOf(counts[i]);
            g2.drawString(cnt, x + (barW - fm.stringWidth(cnt)) / 2, y - 3);

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 8f));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(80, 80, 80));
            String lbl = statuses[i].replace("In Progress", "In Prog.");
            g2.drawString(lbl,
                    x + (barW - fm.stringWidth(lbl)) / 2,
                    mT + cH + 13);
        }
    }

    /**
     * Pinta el gráfico de línea con área rellena (tickets creados, últimos 7 días).
     *
     * @param g2 contexto gráfico
     * @param w  ancho disponible
     * @param h  altura disponible
     */
    private void paintLine(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        LinkedHashMap<String, Long> data   = ticketsByDay();
        List<String>                days   = new ArrayList<>(data.keySet());
        List<Long>                  values = new ArrayList<>(data.values());
        int n = days.size();
        if (n < 2) return;

        int mL = 28, mB = 28, mT = 14, mR = 14;
        int cW = w - mL - mR, cH = h - mB - mT;
        if (cW < 10 || cH < 10) return;

        long maxVal = values.stream().max(Long::compare).orElse(1L);
        if (maxVal == 0) maxVal = 1;

        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = mL + (int)(cW * i / (double)(n - 1));
            ys[i] = mT + cH - (int)(cH * values.get(i) / (double) maxVal);
        }

        Polygon area = new Polygon();
        area.addPoint(xs[0], mT + cH);
        for (int i = 0; i < n; i++) area.addPoint(xs[i], ys[i]);
        area.addPoint(xs[n - 1], mT + cH);
        g2.setColor(new Color(54, 160, 100, 45));
        g2.fillPolygon(area);

        g2.setStroke(new BasicStroke(2f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(54, 160, 100));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 9f));
        for (int i = 0; i < n; i++) {
            g2.setColor(new Color(54, 160, 100));
            g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            g2.setColor(TEXT_DARK);
            FontMetrics fm = g2.getFontMetrics();
            String val = String.valueOf(values.get(i));
            g2.drawString(val, xs[i] - fm.stringWidth(val) / 2, ys[i] - 6);
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        g2.setColor(new Color(100, 100, 100));
        for (int i = 0; i < n; i++) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(days.get(i),
                    xs[i] - fm.stringWidth(days.get(i)) / 2,
                    mT + cH + 13);
        }

        g2.setFont(g2.getFont().deriveFont(9f));
        g2.setColor(new Color(150, 150, 150));
        int ySteps = (int) Math.min(maxVal, 4);
        for (int i = 0; i <= ySteps; i++) {
            long val = maxVal * i / ySteps;
            int  y   = mT + cH - (int)(cH * val / (double) maxVal);
            g2.drawString(String.valueOf(val), 2, y + 4);
        }
    }
}
