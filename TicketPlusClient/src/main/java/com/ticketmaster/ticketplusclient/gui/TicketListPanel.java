/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import com.ticketmaster.ticketplusclient.session.TicketService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.border.MatteBorder;

/**
 * <p>Panel central de tickets que muestra informacion diferente dependiendo
 * del rol Agente y Usuario</p>
 * <ul>
 *   <li><b>ADMIN (Agente):</b> ve todos los tickets del sistema, con opciones
 *       completas de gestión en el menú contextual.</li>
 *   <li><b>USER (Usuario normal):</b> solo ve sus propios tickets abiertos,
 *       en progreso o pendientes. El menú contextual muestra únicamente
 *       las opciones permitidas para su rol.</li>
 * </ul>
 * 
 * @author Christian
 */
public class TicketListPanel extends JPanel{
    
    private static final Color BG_DARK = new Color(21,  25,  28);
    private static final Color BG_MID = new Color(34,  40,  44);
    private static final Color BG_ROW = new Color(28,  33,  37);
    private static final Color BG_ROW_HOVER = new Color(40,  47,  54);
    private static final Color ACCENT_BLUE = new Color(54,  81,  207);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(160, 160, 160);
    private static final Color TEXT_DIM = new Color(100, 100, 100);
    private static final Color PRIO_HIGH = new Color(220, 60,  60);
    private static final Color PRIO_MEDIUM = new Color(220, 160, 40);
    private static final Color PRIO_LOW = new Color(60,  180, 80);

    private static final int PAGE_SIZE = 8;
    private static final int STATS_HEIGHT_MAX = 100; 
    private static final int STATS_HEIGHT_MIN = 0;
    private static final int ANIM_STEP = 8;   
    private static final int ANIM_DELAY_MS = 12;  
    
    private final List<TicketRow> allTickets = new ArrayList<>();
    private List<TicketRow> filtered = new ArrayList<>();
    private String activeFilter = "All";
    private int currentPage = 0;
    private boolean statsExpanded = true;
    private Timer animTimer;
    
    private JPanel statsPanel;
    private JButton toggleBtn;
    private JPanel ticketContainer;
    private JLabel pageLabel;
    private JButton prevBtn;
    private JButton nextBtn;
    
    private final TicketService ticketService = new TicketService();
    
    private final Runnable onNewTicket;
    
    private final Consumer<Long> onShowDetail;
    
    /**
     * Crea el panel de lista de Tickets.
     * 
     */
    public TicketListPanel(Runnable onNewTicket, Consumer<Long> onShowDetail){
        
        this.onNewTicket = onNewTicket;
        this.onShowDetail = onShowDetail;
        
        setLayout(new BorderLayout(0,0));
        setBackground(BG_MID);
        
        add(buildTopSection(), BorderLayout.NORTH);
        add(buildTicketSection(), BorderLayout.CENTER);
        
        loadTicketsFromServer();
    }
    
    // -------------------------------------------------------------------------
    // Sección superior: estadísticas + botón toggle
    // -------------------------------------------------------------------------
    
    /**
     * Define la seccion superior que contiene la barra de estastisticas y la 
     * tira con el boton de colapso/expansion
     * 
     * @return panel con la barra de stats y el toggle
     */
    private JPanel buildTopSection(){
        JPanel wrapper = new JPanel(new BorderLayout(0,0));
        wrapper.setBackground(BG_MID);
        
        statsPanel = buildStatsPanel();
        statsPanel.setPreferredSize(new Dimension(0, STATS_HEIGHT_MAX));
        wrapper.add(statsPanel, BorderLayout.CENTER);
        
        wrapper.add(buildToggleStrip(), BorderLayout.SOUTH);
        return wrapper;
    }
    
    /**
     * Crea la barra de tarjetas de estadisticas
     * 
     * <p>Los contadores se calculan en base a la lista visible del rol actual.</p>
     * 
     * @return panel con las tarjetas
     */
    private JPanel buildStatsPanel(){
        List<TicketRow> visible = getVisibleTickets();
        
        JPanel panel = new JPanel(new GridLayout(1,4,10,0));
        panel.setBackground(BG_MID);
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,8,12));
        
        panel.add(buildStatCard("Open Tickets", countByStatus(visible, "Opened"), new Color(207,97,54)));
        panel.add(buildStatCard("In Progress", countByStatus(visible, "In Progress"), new Color(54,130,207)));
        panel.add(buildStatCard("Answer Pending", countByStatus(visible, "Pending"), new Color(207,160,54)));
        panel.add(buildStatCard("Solved", countByStatus(visible, "Solved"), new Color(54,160,100)));
        
        return panel;
    }
    
    /**
     * Construye una tarjeta individual de estadística con acento de color lateral.
     *
     * @param title  título descriptivo de la métrica
     * @param count  valor numérico a mostrar
     * @param accent color del borde izquierdo de acento
     * @return panel con la tarjeta de estadística
     */
    private JPanel buildStatCard(String title, int count, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(BG_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, accent),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        card.setPreferredSize(new Dimension(0, 80));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setFont(titleLbl.getFont().deriveFont(11f));

        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setForeground(TEXT_WHITE);
        countLbl.setFont(countLbl.getFont().deriveFont(Font.BOLD, 26f));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(countLbl, BorderLayout.CENTER);
        return card;
    }
    
    /**
     * Construye la tira que contiene el botón de colapso/expansión
     * de la barra de estadísticas.
     *
     * @return panel con el botón toggle centrado
     */
    private JPanel buildToggleStrip(){
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));
        strip.setBackground(new Color(28,33,37));
        strip.setPreferredSize(new Dimension(0,18));
        
        toggleBtn = new JButton("▲  Ocultar estadísticas");
        toggleBtn.setBackground(new Color(28,33,37));
        toggleBtn.setForeground(TEXT_DIM);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFont(toggleBtn.getFont().deriveFont(10f));
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> toggleStats());
        toggleBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {toggleBtn.setForeground(TEXT_MUTED);}
            @Override public void mouseExited(MouseEvent e) {toggleBtn.setForeground(TEXT_DIM);}
        });
        
        strip.add(toggleBtn);
        return strip;
    }
    
    // -------------------------------------------------------------------------
    // Animación toggle de estadísticas
    // -------------------------------------------------------------------------
    
    /**
     * Alterna el estado de la barra de estadisticas entre expandido y colapsado.
     * 
     */
    private void toggleStats(){
        if(animTimer != null && animTimer.isRunning()) animTimer.stop();
        
        statsExpanded = !statsExpanded;
        
        int target = statsExpanded ? STATS_HEIGHT_MAX : STATS_HEIGHT_MIN;
        
        toggleBtn.setText(statsExpanded
            ? "▲  Ocultar estadísticas"
                : "▼  Mostrar estadísticas");
        
        animTimer = new Timer(ANIM_DELAY_MS, null);
        animTimer.addActionListener(e -> {
            int current = statsPanel.getPreferredSize().height;
            int next = statsExpanded
                ? Math.min(current + ANIM_STEP, target)
                : Math.max(current - ANIM_STEP, target);
            
            statsPanel.setPreferredSize(new Dimension(0, next));
            statsPanel.setVisible(next > 0);
            revalidate();
            
            if(next == target) animTimer.stop();
        });
        
        animTimer.start(); 
    }
    
    // -------------------------------------------------------------------------
    // Sección de tickets: toolbar + lista + paginación
    // -------------------------------------------------------------------------
    
    /**
     * Crea la seccion inferior que contendra la barra de herramientas con (New y Filter),
     * la lista de tickets y la paginacion
     * 
     * @return panel con la seccion inferior
     */
    private JPanel buildTicketSection(){
        JPanel panel = new JPanel(new BorderLayout(0,0));
        panel.setBackground(BG_MID);
        
        panel.add(buildToolbar(), BorderLayout.NORTH);
        panel.add(buildTicketList(), BorderLayout.CENTER);
        panel.add(buildPagination(), BorderLayout.SOUTH);
        
        return panel;
    }
    
    // -------------------------------------------------------------------------
    // Barra de herramientas
    // -------------------------------------------------------------------------
 
    /**
     * Crea la barra de herramientas con el boton de crear ticket a la izquierda
     * y el boton de filtro a la derecha.
     * 
     * @return 
     */
    private JPanel buildToolbar(){
        JPanel bar = new JPanel(new BorderLayout(0,0));
        bar.setBackground(BG_MID);
        bar.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        
        JButton newBtn = new JButton("New +");
        newBtn.setBackground(ACCENT_BLUE);
        newBtn.setForeground(TEXT_WHITE);
        newBtn.setFocusPainted(false);
        newBtn.setFont(newBtn.getFont().deriveFont(Font.BOLD, 12f));
        newBtn.setBorder(BorderFactory.createEmptyBorder(6,18,6,18));
        newBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newBtn.addMouseListener(hoverEffect(newBtn, ACCENT_BLUE, ACCENT_BLUE.darker()));
        newBtn.addActionListener(e -> onNewTicket.run());
        
        bar.add(newBtn, BorderLayout.WEST);
        bar.add(buildFilterBar(), BorderLayout.EAST);
        
        return bar;
        
    }
    
    /**
     * Genera el panel de filtro de estado
     * 
     * @return panel con los botones de filtro alineados a la derecha
     */
    private JPanel buildFilterBar(){
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panel.setBackground(BG_MID);
        
        for(String f: new String[]{"All", "Opened", "In Progress", "Pendind", "Solved"}){
            panel.add(buildFilterButton(f));
        }
        
        return panel;
    }
    
    private JButton buildFilterButton(String label){
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(60,70,80) , 1, true),
                BorderFactory.createEmptyBorder(4,14,4,14)
        ));
        btn.setBackground(label.equals(activeFilter) ? ACCENT_BLUE : BG_DARK);
        btn.setForeground(label.equals(activeFilter)? TEXT_WHITE : TEXT_MUTED);
        btn.addActionListener(e -> applyFilter(label));
        
        return btn;
    }
    
    // -------------------------------------------------------------------------
    // Lista de tickets
    // -------------------------------------------------------------------------
    
    /**
     * Construye el {@link JScrollPane} que contiene el conteneder de filas de tickets. 
     * El scroll horizontal esta deshabilitado
     * 
     * @return scroll pane con el contenedor de tickets
     */
    private JScrollPane buildTicketList(){
        ticketContainer = new JPanel();
        ticketContainer.setLayout(new BoxLayout(ticketContainer, BoxLayout.Y_AXIS));
        ticketContainer.setBackground(BG_MID);
        ticketContainer.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
        
        JScrollPane scroll = new JScrollPane(ticketContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_MID);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        return scroll;
    }
    
    /**
    * Recarga los tickets desde el servidor y reaplica el filtro activo.
    * Debe llamarse tras crear o modificar un ticket para mantener la lista actualizada.
    */
    public void refresh() {
        loadTicketsFromServer();
    }
    
    // -------------------------------------------------------------------------
    // Paginación
    // -------------------------------------------------------------------------
    
    /**
     * Crea la barra de paginacion con los botones de navegacion y la etiqueta
     * de la pagina actual
     * 
     * @return panel con los controles de paginacion
     */
    private JPanel buildPagination(){
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setBackground(BG_MID);
        
        prevBtn = buildNavButton("<");
        prevBtn.addActionListener(e -> {
            if(currentPage > 0) { currentPage--; renderPage();}
        });
        
        pageLabel = new JLabel("1..1");
        pageLabel.setForeground(TEXT_MUTED);
        pageLabel.setFont(pageLabel.getFont().deriveFont(12f));
        
        nextBtn = buildNavButton(">");
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages() - 1) { currentPage++; renderPage();}
        });
        
        bar.add(prevBtn);
        bar.add(pageLabel);
        bar.add(nextBtn);
        
        return bar;
    }
    
    /**
     * Crea un boton de navegacion
     * 
     * @param label simbolo del boton (< o >)
     * @return boton de navegacion
     */
    private JButton buildNavButton(String label){
        JButton btn = new JButton(label);
        btn.setBackground(BG_DARK);
        btn.setForeground(TEXT_MUTED);
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(16f));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,70,80), 1, true),
                BorderFactory.createEmptyBorder(2,14,2,14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return btn;
    }
    
    // -------------------------------------------------------------------------
    // Lógica de filtrado y renderizado
    // -------------------------------------------------------------------------
    
    /**
     * Devuelve la listande ticket que el rol actual puede ver
     * 
     * <ul>
     *   <li><b>ADMIN:</b> todos los tickets del sistema.</li>
     *   <li><b>USER:</b> únicamente sus propios tickets en estado abierto,
     *       en progreso o pendiente de respuesta.</li>
     * </ul>
     * 
     * @return lista de {@link TicketRow} visible para el rol actual
     */
    private List<TicketRow> getVisibleTickets(){
        SessionManager session = SessionManager.getInstance();
        
        if(session.isAdmin()) {
            return new ArrayList<>(allTickets);
        }
        
        String username = session.getUsername();
        
        System.out.println(">>> SESSION username: [" + username + "]");
        allTickets.forEach(t -> 
            System.out.println(">>> TICKET createdBy: [" + t.createdBy + "]"));
    
        List<TicketRow> visible = new ArrayList<>();
        for(TicketRow t : allTickets){
            boolean isOwner = username.equals(t.createdBy);
            boolean isOpen = "Opened".equals(t.status)
                    || "In Progress".equals(t.status)
                    || "Pending".equals(t.status);
            if(isOwner && isOpen) visible.add(t);
        }
        
        return visible;
        
    }
    
    /**
     * Aplica el filtro de estado seleccionado sobre la lista visible para el rol actual
     * 
     * @param filter estado por el que se filtra
     */
    private void applyFilter(String filter){
        activeFilter = filter;
        currentPage = 0;
        
        filtered = new ArrayList<>();
        for(TicketRow t : getVisibleTickets()){
            if("All".equals(filter) || filter.equals(t.status)) filtered.add(t);
        }
        
        refreshFilterBar();
        renderPage();
    }
    
    
    /**
     * Reconstruye los botones de la barra de filtros para mostrar el filtro activo actual
     * 
     */
    private void refreshFilterBar(){
        Component ticketSection = ((BorderLayout) getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if(!(ticketSection instanceof JPanel ts)) return;
        
        Component toolbar = ((BorderLayout) ts.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        if(!(toolbar instanceof JPanel tb)) return;
        
        Component east = ((BorderLayout) tb.getLayout())
                .getLayoutComponent(BorderLayout.EAST);
        if(!(east instanceof JPanel filterBar)) return;
        
        filterBar.removeAll();
        for(String f : new String[]{"All", "Opened", "In Progress", "Pending", "Solved"}){
            filterBar.add(buildFilterButton(f));
        }
        filterBar.revalidate();
        filterBar.repaint();   
    }
    
    /**
     * Renderiza la pagina actual de tickets en el contenedor
     * 
     */
    private void renderPage(){
        ticketContainer.removeAll();
        
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filtered.size());
        int total = totalPages();
        
        for(int i = from; i < to; i++){
            ticketContainer.add(buildTicketRow(filtered.get(i)));
            ticketContainer.add(Box.createRigidArea(new Dimension(0,6)));
        }
        
        if(filtered.isEmpty()){
            JLabel empty = new JLabel("No se encontraron tickets");
            empty.setForeground(TEXT_DIM);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(40,0,0,0));
            ticketContainer.add(empty);
        }
        
        pageLabel.setText((currentPage + 1) + ".." + Math.max(1, total));
        prevBtn.setEnabled(currentPage > 0);
        nextBtn.setEnabled(currentPage < total - 1);
        
        ticketContainer.revalidate();
        ticketContainer.repaint();
    }
    
    // -------------------------------------------------------------------------
    // Fila de ticket individual
    // -------------------------------------------------------------------------
    
    
    /**
     * Crea la fila de ticket individual
     * 
     * <p>La fila se divide en cuatro secciones horizontales:</p>
     * <ol>
     *   <li>Izquierda: referencia y título del ticket.</li>
     *   <li>Centro: encabezado y texto de descripción.</li>
     *   <li>Prioridad: punto de color indicando la prioridad.</li>
     *   <li>Derecha: metadatos (creador, estado, agente) y botón de acción.</li>
     * </ol>
     * 
     * @param ticket datos del ticket a mostrar
     * @return panel con la fila completa del ticket
     */
    private JPanel buildTicketRow(TicketRow ticket){
        JPanel row = new JPanel(new BorderLayout(0,0));
        row.setBackground(BG_ROW);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45,52,58), 1),
                BorderFactory.createEmptyBorder(10,0,10,0)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        row.setCursor(Cursor. getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(BG_ROW_HOVER);}
            @Override public void mouseExited(MouseEvent e) { row.setBackground(BG_ROW);}            
        });
        
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(0,14,0,0));
        left.setPreferredSize(new Dimension(200,0));
        
        JLabel refLbl = new JLabel(ticket.ref);
        refLbl.setForeground(TEXT_WHITE);
        refLbl.setFont(refLbl.getFont().deriveFont(Font.BOLD, 12f));
        
        JLabel titleLbl = new JLabel("Title: "+ticket.title);
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setFont(titleLbl.getFont().deriveFont(11f));
        
        left.add(refLbl);
        left.add(Box.createRigidArea(new Dimension(0,4)));
        left.add(titleLbl);
        
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0,1,0,1, new Color(50,57,64)),
                BorderFactory.createEmptyBorder(0,14,0,14)
        ));
        
        JLabel descHeader = new JLabel("Description");
        descHeader.setForeground(TEXT_MUTED);
        descHeader.setFont(descHeader.getFont().deriveFont(Font.BOLD, 11f));
        
        
        JLabel descText = new JLabel(ticket.description);
        descText.setForeground(TEXT_MUTED);
        descText.setFont(descText.getFont().deriveFont(11f));
        
        center.add(descHeader);
        center.add(Box.createRigidArea(new Dimension(0, 4)));
        center.add(descText);

        JPanel prioPanel = new JPanel(new GridBagLayout());
        prioPanel.setOpaque(false);
        prioPanel.setPreferredSize(new Dimension(100, 0));
        prioPanel.setBorder(new MatteBorder(0, 0, 0, 1, new Color(50, 57, 64)));
        
        JPanel prioContent = new JPanel();
        prioContent.setOpaque(false);
        prioContent.setLayout(new GridBagLayout());
       
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 4, 0);

        JLabel prioLabel = new JLabel("Priority");
        prioLabel.setForeground(TEXT_MUTED);
        prioLabel.setFont(prioLabel.getFont().deriveFont(Font.BOLD, 11f));

        gbc.gridy = 0;
        prioContent.add(prioLabel, gbc);
        
        PrioDot dot = new PrioDot(priorityColor(ticket.priority));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        prioContent.add(dot, gbc);
        prioPanel.add(prioContent);
        
        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
        right.setPreferredSize(new Dimension(210, 0));
        
        JPanel metaPanel = new JPanel();
        metaPanel.setOpaque(false);
        metaPanel.setLayout(new BoxLayout(metaPanel, BoxLayout.Y_AXIS));
        
        JLabel byLbl = new JLabel("By: " + ticket.createdBy);
        JLabel statusLbl = new JLabel("Status: " + ticket.status);
        JLabel agentLbl = new JLabel("Agent: " + ticket.agent);
        
        for (JLabel lbl : new JLabel[]{byLbl, statusLbl, agentLbl}) {
            lbl.setForeground(lbl == statusLbl ? statusColor(ticket.status) : TEXT_MUTED);
            lbl.setFont(lbl.getFont().deriveFont(11f));
        }
        
        metaPanel.add(byLbl);
        metaPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        metaPanel.add(statusLbl);
        metaPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        metaPanel.add(agentLbl);
        
        right.add(metaPanel, BorderLayout.CENTER);
        right.add(buildActionButton(ticket), BorderLayout.EAST);
        
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.add(prioPanel, BorderLayout.WEST);
        rightWrapper.add(right, BorderLayout.EAST);
        
        row.add(left,  BorderLayout.WEST);
        row.add(center, BorderLayout.CENTER);
        row.add(rightWrapper, BorderLayout.EAST);
        
        return row;
    }
    
    /**
     * Construye el boton de acción "···" de una fila de ticket.
     *
     * @param ticket ticket al que pertenece el botón
     * @return botón configurado con su listener de menú contextual
     */
    private JButton buildActionButton(TicketRow ticket) {
        JButton btn = new JButton("···");
        btn.setBackground(new Color(50, 57, 64));
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showTicketMenu(btn, ticket));
        return btn;
    }

    /**
     * Muestra el menú contextual de acciones para un ticket.
     *
     * <p>El contenido del menú varía según el rol del usuario autenticado:</p>
     * <ul>
     *   <li><b>ADMIN:</b> Ver detalles, Asignarme, Cambiar estado, Cerrar ticket.</li>
     *   <li><b>USER:</b> Ver detalles, Cerrar ticket.</li>
     * </ul>
     *
     * @param source botón sobre el que se anclará el menú
     * @param ticket ticket al que corresponden las acciones
     */
    private void showTicketMenu(JButton source, TicketRow ticket) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(BG_DARK);

        boolean isAdmin = SessionManager.getInstance().isAdmin();

        addMenuItem(menu, "Ver detalles", () -> onShowDetail.accept(ticket.id));
        
                
        if (isAdmin) {
            addMenuItem(menu, "Asignarme", () ->
                ticketService.assignToMe(ticket.id, new TicketService.ServiceCallback<TicketDTO>() {
                    @Override public void onSuccess(TicketDTO t) { loadTicketsFromServer(); }
                    @Override public void onError(String e) { JOptionPane.showMessageDialog(null, e); }
                }));
                
            addMenuItem(menu, "Cambiar estado", () -> {
                String[] options = {"In Progress", "Pending", "Solved", "Closed"};
                String chosen = (String) JOptionPane.showInputDialog(
                    TicketListPanel.this, "Nuevo estado:", "Cambiar estado",
                    JOptionPane.PLAIN_MESSAGE, null, options, ticket.status);
                if(chosen != null)
                    ticketService.changeStatus(ticket.id, chosen,
                            new TicketService.ServiceCallback<TicketDTO>(){
                                @Override public void onSuccess(TicketDTO t){loadTicketsFromServer();}
                                @Override public void onError(String e){JOptionPane.showMessageDialog(null, e);}
                                
                            });
            });

            addMenuItem(menu, "Cerrar ticket", () -> 
            ticketService.closeTicket(ticket.id, new TicketService.ServiceCallback<TicketDTO>(){
                @Override public void onSuccess(TicketDTO t) { loadTicketsFromServer(); }
                @Override public void onError(String e)      { JOptionPane.showMessageDialog(null, e); }
            }));
        }    

        menu.show(source, 0, source.getHeight());
    }

    /**
     * Crea un {@link JMenuItem} con estilo oscuro, lo conecta a la acción
     * indicada y lo añade al menú.
     *
     * @param menu menú contextual donde se añadirá el ítem
     * @param label texto visible del ítem
     * @param action acción a ejecutar al hacer clic
     */
    private void addMenuItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.setBackground(BG_DARK);
        item.setForeground(TEXT_WHITE);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    // -------------------------------------------------------------------------
    // Componente punto de prioridad
    // -------------------------------------------------------------------------

    /**
     * Componente gráfico que genera el circulo de colores con la prioridad del
     * Ticket
     * 
     */
    private static class PrioDot extends JPanel {

        private final Color color;

        /**
         * Crea un punto de prioridad con el color especificado.
         *
         * @param color color del círculo
         */
        PrioDot(Color color) {
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(14, 14));
            setMinimumSize(new Dimension(14, 14));
            setMaximumSize(new Dimension(14, 14));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Pinta el círculo con antialiasing para bordes suaves.</p>
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, 14, 14);
            g2.dispose();
        }
    }

    /**
     * Carga la lista de tickets desde el servidor mediante {@link TicketService}.
     *
     * <p>Al completar con éxito, actualiza {@link #allTickets} y reaplica
     * el filtro activo. En caso de error muestra un diálogo informativo.</p>
     */
    private void loadTicketsFromServer(){
        ticketService.getTickets(new TicketService.ServiceCallback<List<TicketDTO>>() {
            @Override
            public void onSuccess(List<TicketDTO> tickets){
                System.out.println(">>> onSuccess llamado. Tickets recibidos: " + tickets.size());
                tickets.forEach(t -> System.out.println(">>> DTO createdBy: [" + t.getCreatedBy() + "] status: [" + t.getStatus() + "]"));

                allTickets.clear();
                for(TicketDTO dto: tickets){
                    allTickets.add(new TicketRow(
                            dto.getId(),
                            dto.getRef(),
                            dto.getTitle(),
                            dto.getDescription(),
                            dto.getPriority(),
                            dto.getStatus(),
                            dto.getCreatedBy(),
                            dto.getAgent() != null ? dto.getAgent() : "Sin asignar",
                            dto.getCreatedAt()
                            
                    ));
                }
                // LOG TEMPORAL — verificar que los datos llegan
                System.out.println(">>> Tickets cargados: " + allTickets.size());
                allTickets.forEach(t ->
                    System.out.println(">>> TICKET createdBy: [" + t.createdBy + "]"));
                applyFilter(activeFilter);
            }
            @Override
            public void onError(String errorMessage){
                System.out.println(">>> onError: " + errorMessage);
                JOptionPane.showMessageDialog(TicketListPanel.this, errorMessage,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares
    // -------------------------------------------------------------------------

    /**
     * Cuenta los tickets de una lista que tienen un estado determinado.
     *
     * @param list lista de tickets sobre la que contar
     * @param status estado por el que filtrar
     * @return número de tickets con ese estado
     */
    private int countByStatus(List<TicketRow> list, String status) {
        return (int) list.stream().filter(t -> status.equals(t.status)).count();
    }

    /**
     * Calcula el número total de páginas para la lista filtrada actual.
     *
     * @return número de páginas (mínimo 1)
     */
    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
    }

    /**
     * Devuelve el color correspondiente a un nivel de prioridad.
     *
     * @param p nivel de prioridad ("HIGH", "MEDIUM" o "LOW")
     * @return color asociado a la prioridad
     */
    private Color priorityColor(String p) {
        return switch (p) {
            case "HIGH"   -> PRIO_HIGH;
            case "MEDIUM" -> PRIO_MEDIUM;
            default       -> PRIO_LOW;
        };
    }

    /**
     * Devuelve el color correspondiente a un estado de ticket.
     *
     * @param s estado del ticket
     * @return color semántico asociado al estado
     */
    private Color statusColor(String s) {
        return switch (s) {
            case "Opened"      -> new Color(207, 97,  54);
            case "In Progress" -> new Color(54,  130, 207);
            case "Pending"     -> new Color(207, 160, 54);
            case "Solved"      -> new Color(54,  160, 100);
            default            -> TEXT_MUTED;
        };
    }

    /**
     * Crea un {@link MouseAdapter} que aplica un efecto hover de color
     * sobre un botón.
     *
     * @param btn botón al que se aplicará el efecto
     * @param normal color del botón en estado normal
     * @param hover color del botón al pasar el ratón por encima
     * @return adaptador de ratón configurado
     */
    private MouseAdapter hoverEffect(JButton btn, Color normal, Color hover) {
        return new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(normal); }
        };
    }

    // -------------------------------------------------------------------------
    // Modelo de datos
    // -------------------------------------------------------------------------

    /**
     * Modelo inmutable que representa un ticket en la lista.
     *
     * <p>Todos los campos son públicos y finales para facilitar el acceso
     * desde los renderizadores de fila sin necesidad de getters.</p>
     */
    public static class TicketRow {
        
        public final Long id;

        /** Referencia única del ticket. */
        public final String ref;

        /** Título corto del ticket. */
        public final String title;

        /** Descripción breve del problema. */
        public final String description;

        /** Nivel de prioridad: "HIGH", "MEDIUM" o "LOW". */
        public final String priority;

        /** Estado actual: "Opened", "In Progress", "Pending" o "Solved". */
        public final String status;

        /** Nombre del usuario que abrió el ticket. */
        public final String createdBy;

        /** Nombre del agente asignado al ticket. */
        public final String agent;
        
        public final String createdAt;

        /**
         * Crea un nuevo registro de ticket con todos sus campos.
         *
         * @param id
         * @param ref referencia única
         * @param title título del ticket
         * @param description descripción breve
         * @param priority nivel de prioridad
         * @param status estado actual
         * @param createdBy usuario que lo abrió
         * @param agent agente asignado
         */
        public TicketRow(Long id, String ref, String title, String description,
                         String priority, String status,
                         String createdBy, String agent, String createdAt) {
            this.id = id;
            this.ref = ref;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.status = status;
            this.createdBy = createdBy;
            this.agent = agent;
            this.createdAt = createdAt;
        }
    }
    
}
