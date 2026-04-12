/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.CommentDTO;
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
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

/**
 *
 * @author Christian G
 */
public class TicketDetailPanel extends JPanel {

    // ─── Colores ──────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(21,  25,  28);
    private static final Color BG_MID     = new Color(34,  40,  44);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color ACCENT     = new Color(54,  81,  207);
    private static final Color TEXT_DARK  = new Color(30,  30,  30);
    private static final Color TEXT_MUTED = new Color(120, 120, 120);
    private static final Color PRIO_HIGH  = new Color(220, 60,  60);
    private static final Color PRIO_MED   = new Color(220, 160, 40);
    private static final Color PRIO_LOW   = new Color(60,  180, 80);

    private static final int PAGE_SIZE = 5;

    private final TicketService ticketService;
    private final Runnable      onBack;

    private TicketDTO         ticket;
    private List<CommentDTO>  comments;
    private int               currentPage = 0;

    // ─── Componentes dinámicos ────────────────────────────────
    private JPanel        commentContainer;
    private JLabel        pageLabel;
    private JButton       prevBtn;
    private JButton       nextBtn;
    private JComboBox<String> statusCombo;
    private JComboBox<String> agentCombo;
    private JComboBox<String> priorityCombo;
    private JPanel        prioDot;

    /**
     * Crea el panel de detalle para el ticket indicado.
     *
     * @param ticketId identificador del ticket a mostrar
     * @param onBack   acción al pulsar "Back" (vuelve a la lista)
     */
    public TicketDetailPanel(Long ticketId, Runnable onBack) {
        this.ticketService = new TicketService();
        this.onBack        = onBack;

        setLayout(new BorderLayout());
        setBackground(BG_MID);

        // Mostrar loading mientras carga
        JLabel loading = new JLabel("Cargando...", SwingConstants.CENTER);
        loading.setForeground(Color.WHITE);
        add(loading, BorderLayout.CENTER);

        loadTicketAndComments(ticketId);
    }

    // ─── Carga de datos ───────────────────────────────────────

    /**
     * Carga el ticket y sus comentarios desde el servidor y construye la UI.
     *
     * @param ticketId identificador del ticket
     */
    private void loadTicketAndComments(Long ticketId) {
        ticketService.getTicket(ticketId, new TicketService.ServiceCallback<TicketDTO>() {
            @Override
            public void onSuccess(TicketDTO result) {
                ticket = result;
                ticketService.getComments(ticketId,
                        new TicketService.ServiceCallback<List<CommentDTO>>() {
                    @Override
                    public void onSuccess(List<CommentDTO> result) {
                        comments = result;
                        buildUI();
                    }
                    @Override
                    public void onError(String errorMessage) {
                        showError(errorMessage);
                    }
                });
            }
            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    // ─── Construcción de la UI ────────────────────────────────

    /**
     * Construye toda la interfaz una vez cargados ticket y comentarios.
     */
    private void buildUI() {
        removeAll();
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),     BorderLayout.NORTH);
        add(buildMainArea(),   BorderLayout.CENTER);
        add(buildBottomBar(),  BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    /**
     * Barra superior con el botón "Add Comment".
     *
     * @return panel superior
     */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        bar.setBackground(BG_MID);

        JButton addCommentBtn = new JButton("Add Comment");
        addCommentBtn.setBackground(ACCENT);
        addCommentBtn.setForeground(Color.WHITE);
        addCommentBtn.setFocusPainted(false);
        addCommentBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addCommentBtn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        addCommentBtn.addActionListener(e -> showAddCommentDialog());

        bar.add(addCommentBtn);
        return bar;
    }

    /**
     * Área principal: hilo de comentarios a la izquierda y sidebar a la derecha.
     *
     * @return panel principal dividido
     */
    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout(10, 0));
        area.setBackground(BG_MID);
        area.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        
        JPanel leftPanel = new JPanel(new BorderLayout(0,8));
        leftPanel.setBackground(BG_MID);
        leftPanel.add(buildTicketInfoCard(), BorderLayout.NORTH); 
        leftPanel.add(buildCommentSection(), BorderLayout.CENTER);

        area.add(leftPanel, BorderLayout.CENTER);
        area.add(buildSidebar(),        BorderLayout.EAST);

        return area;
    }
    
    /**
    * Construye la tarjeta superior con la referencia, título y descripción
    * del ticket actual.
    *
    * @return panel con la información principal del ticket
    */
    private JPanel buildTicketInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Cabecera: referencia + fecha de creación
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(0, 0, 8, 0)
        ));

        JLabel refLabel = new JLabel("#" + ticket.getRef());
        refLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        refLabel.setForeground(TEXT_DARK);

        JLabel dateLabel = new JLabel(formatDate(ticket.getCreatedAt()));
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_MUTED);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(refLabel,  BorderLayout.WEST);
        header.add(dateLabel, BorderLayout.EAST);

        // Título
        JLabel titleLabel = new JLabel(ticket.getTitle());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(TEXT_DARK);

        // Descripción con wrap automático via HTML
        JLabel descLabel = new JLabel(
            "<html><body style='width:100%'>" + ticket.getDescription() + "</body></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_MUTED);

        // Cuerpo: título + descripción
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setBackground(BG_CARD);
        body.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        body.add(titleLabel, BorderLayout.NORTH);
        body.add(descLabel,  BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(body,   BorderLayout.CENTER);

        return card;
    }

    /**
     * Sección del hilo de comentarios con scroll y paginación.
     *
     * @return panel con el contenedor de comentarios
     */
    private JPanel buildCommentSection() {
        JPanel section = new JPanel(new BorderLayout(0, 0));
        section.setBackground(BG_MID);

        commentContainer = new JPanel();
        commentContainer.setLayout(new BoxLayout(commentContainer, BoxLayout.Y_AXIS));
        commentContainer.setBackground(BG_MID);

        JScrollPane scroll = new JScrollPane(commentContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_MID);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        section.add(scroll, BorderLayout.CENTER);
        section.add(buildPagination(), BorderLayout.SOUTH);

        renderCommentPage();
        return section;
    }

    /**
     * Renderiza la página actual del hilo de comentarios.
     */
    private void renderCommentPage() {
        commentContainer.removeAll();

        int from  = currentPage * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, comments.size());
        int total = totalPages();

        for (int i = from; i < to; i++) {
            commentContainer.add(buildCommentCard(comments.get(i)));
            commentContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        if (comments.isEmpty()) {
            JLabel empty = new JLabel("No hay comentarios aún.");
            empty.setForeground(TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            commentContainer.add(empty);
        }

        pageLabel.setText((currentPage + 1) + "..." + Math.max(1, total));
        prevBtn.setEnabled(currentPage > 0);
        nextBtn.setEnabled(currentPage < total - 1);

        commentContainer.revalidate();
        commentContainer.repaint();
    }

    /**
     * Construye la tarjeta visual de un comentario individual.
     *
     * @param comment comentario a representar
     * @return panel con la tarjeta del comentario
     */
    private JPanel buildCommentCard(CommentDTO comment) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Cabecera de la tarjeta ────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JPanel headerLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        headerLeft.setBackground(BG_CARD);

        JLabel refLabel   = new JLabel("#" + ticket.getRef() + ": " + "1001");
        JLabel titleLabel = new JLabel("Title: " + ticket.getTitle());
        refLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        refLabel.setForeground(TEXT_DARK);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_MUTED);
        headerLeft.add(refLabel);
        headerLeft.add(titleLabel);

        JPanel headerRight = new JPanel(new GridLayout(2, 1, 0, 2));
        headerRight.setBackground(BG_CARD);

        JLabel byLabel   = new JLabel("By: " + comment.getAuthor());
        JLabel dateLabel = new JLabel("Data: " + formatDate(comment.getCreatedAt()));
        byLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        byLabel.setForeground(TEXT_MUTED);
        byLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_MUTED);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        headerRight.add(byLabel);
        headerRight.add(dateLabel);

        header.add(headerLeft,  BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // ── Cuerpo de la tarjeta ──────────────────────────────
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setBackground(BG_CARD);
        body.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel descLabel = new JLabel("Description");
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        descLabel.setForeground(TEXT_DARK);

        JLabel contentLabel = new JLabel("<html>" + comment.getContent() + "</html>");
        contentLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        contentLabel.setForeground(TEXT_MUTED);

        body.add(descLabel,   BorderLayout.NORTH);
        body.add(contentLabel, BorderLayout.CENTER);

        // ── Botón ··· solo en comentarios de otros (ADMIN puede responder) ──
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        if (isAdmin) {
            JButton dotsBtn = new JButton("···");
            dotsBtn.setBackground(new Color(50, 57, 64));
            dotsBtn.setForeground(Color.WHITE);
            dotsBtn.setFocusPainted(false);
            dotsBtn.setFont(dotsBtn.getFont().deriveFont(Font.BOLD, 13f));
            dotsBtn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            dotsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dotsBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Opciones de comentario — próximamente."));
            JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            dotsPanel.setBackground(BG_CARD);
            dotsPanel.add(dotsBtn);
            body.add(dotsPanel, BorderLayout.SOUTH);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(body,   BorderLayout.CENTER);
        return card;
    }

    /**
     * Barra de paginación de comentarios.
     *
     * @return panel con los controles de paginación
     */
    private JPanel buildPagination() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setBackground(BG_MID);

        prevBtn = buildNavButton("<");
        prevBtn.addActionListener(e -> {
            if (currentPage > 0) { currentPage--; renderCommentPage(); }
        });

        pageLabel = new JLabel("1...1");
        pageLabel.setForeground(new Color(160, 160, 160));
        pageLabel.setFont(pageLabel.getFont().deriveFont(12f));

        nextBtn = buildNavButton(">");
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages() - 1) { currentPage++; renderCommentPage(); }
        });

        bar.add(prevBtn);
        bar.add(pageLabel);
        bar.add(nextBtn);
        return bar;
    }

    /**
     * Sidebar derecho con prioridad, creador, status y agente editables según rol.
     *
     * @return panel lateral
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_MID);
        sidebar.setPreferredSize(new Dimension(160, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        boolean isAdmin = SessionManager.getInstance().isAdmin();

        // ── Priority ──────────────────────────────────────────
        sidebar.add(buildSidebarLabel("Priority:"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));

        prioDot = buildPrioDot(ticket.getPriority());
        JPanel prioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        prioRow.setBackground(BG_MID);
        prioRow.add(prioDot);

        priorityCombo = new JComboBox<>(new String[]{"HIGH", "MEDIUM", "LOW"});
        priorityCombo.setSelectedItem(ticket.getPriority());
        priorityCombo.setBackground(new Color(60, 68, 76));
        priorityCombo.setForeground(Color.WHITE);
        priorityCombo.setFocusable(false);
        priorityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        // Ambos roles pueden cambiar prioridad
        priorityCombo.addActionListener(e -> handlePriorityChange());
        prioRow.add(priorityCombo);

        sidebar.add(prioRow);
        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        // ── By ────────────────────────────────────────────────
        sidebar.add(buildSidebarLabel("By:"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
        JLabel byValue = buildSidebarValue(ticket.getCreatedBy());
        byValue.setBackground(new Color(60, 68, 76));
        byValue.setOpaque(true);
        byValue.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        sidebar.add(byValue);
        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        // ── Status ────────────────────────────────────────────
        sidebar.add(buildSidebarLabel("Status:"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));

        if (isAdmin) {
            statusCombo = new JComboBox<>(
                    new String[]{"Opened", "In Progress", "Pending", "Solved", "Closed"});
            statusCombo.setSelectedItem(ticket.getStatus());
            statusCombo.setBackground(new Color(60, 68, 76));
            statusCombo.setForeground(Color.WHITE);
            statusCombo.setFocusable(false);
            statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            statusCombo.addActionListener(e -> handleStatusChange());
            sidebar.add(statusCombo);
        } else {
            // USER solo puede cerrar
            JButton closeBtn = new JButton("Close ticket");
            closeBtn.setBackground(new Color(160, 50, 50));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setFocusPainted(false);
            closeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> handleCloseTicket());
            sidebar.add(closeBtn);
        }

        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        // ── Agent (solo ADMIN) ────────────────────────────────
        if (isAdmin) {
            sidebar.add(buildSidebarLabel("Agent:"));
            sidebar.add(Box.createRigidArea(new Dimension(0, 4)));

            // TODO TEA3: cargar lista de agentes del servidor
            agentCombo = new JComboBox<>(new String[]{"Sin asignar", "admin", "erik", "christian", "david"});//Recuperar de agentes del sistema, por medio de endpoint
            agentCombo.setSelectedItem(ticket.getAgent() != null ? ticket.getAgent() : "Sin asignar");
            agentCombo.setBackground(new Color(60, 68, 76));
            agentCombo.setForeground(Color.WHITE);
            agentCombo.setFocusable(false);
            agentCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            agentCombo.addActionListener(e -> handleAgentChange());
            sidebar.add(agentCombo);
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    /**
     * Barra inferior con el botón "Back".
     *
     * @return panel inferior
     */
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(BG_MID);

        JButton backBtn = new JButton("Back");
        backBtn.setBackground(new Color(70, 80, 90));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        backBtn.addActionListener(e -> onBack.run());

        bar.add(backBtn);
        return bar;
    }

    // ─── Acciones ─────────────────────────────────────────────

    /**
     * Muestra el diálogo para añadir un nuevo comentario y lo envía al servidor.
     */
    private void showAddCommentDialog() {
        JTextArea input = new JTextArea(5, 30);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(input);

        int result = JOptionPane.showConfirmDialog(this, scroll,
                "Nuevo comentario", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String content = input.getText().trim();
            if (content.isEmpty()) return;

            ticketService.addComment(ticket.getId(), content,
                    new TicketService.ServiceCallback<CommentDTO>() {
                @Override
                public void onSuccess(CommentDTO comment) {
                    comments.add(comment);
                    currentPage = totalPages() - 1;
                    renderCommentPage();
                }
                @Override
                public void onError(String errorMessage) {
                    JOptionPane.showMessageDialog(TicketDetailPanel.this,
                            errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    /**
     * Envía el cambio de prioridad al servidor cuando el usuario modifica el combo.
     */
    private void handlePriorityChange() {
        String selected = (String) priorityCombo.getSelectedItem();
        if (selected == null || selected.equals(ticket.getPriority())) return;

        ticketService.changePriority(ticket.getId(), selected,
                new TicketService.ServiceCallback<TicketDTO>() {
            @Override
            public void onSuccess(TicketDTO updated) {
                ticket = updated;
                updatePrioDot(updated.getPriority());
            }
            @Override
            public void onError(String errorMessage) {
                priorityCombo.setSelectedItem(ticket.getPriority());
                JOptionPane.showMessageDialog(TicketDetailPanel.this,
                        errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Envía el cambio de estado al servidor cuando el agente modifica el combo.
     */
    private void handleStatusChange() {
        String selected = (String) statusCombo.getSelectedItem();
        if (selected == null || selected.equals(ticket.getStatus())) return;

        ticketService.changeStatus(ticket.getId(), selected,
                new TicketService.ServiceCallback<TicketDTO>() {
            @Override
            public void onSuccess(TicketDTO updated) { ticket = updated; }
            @Override
            public void onError(String errorMessage) {
                statusCombo.setSelectedItem(ticket.getStatus());
                JOptionPane.showMessageDialog(TicketDetailPanel.this,
                        errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Envía la asignación de agente al servidor cuando el agente modifica el combo.
     */
    private void handleAgentChange() {
        String selected = (String) agentCombo.getSelectedItem();
        if (selected == null || selected.equals(ticket.getAgent())) return;

        if ("Sin asignar".equals(selected)) return;

        ticketService.assignToAgent(ticket.getId(), selected,
                new TicketService.ServiceCallback<TicketDTO>() {
            @Override
            public void onSuccess(TicketDTO updated) {
                ticket = updated;
                if (statusCombo != null)
                    statusCombo.setSelectedItem(updated.getStatus());
            }
            @Override
            public void onError(String errorMessage) {
                agentCombo.setSelectedItem(ticket.getAgent());
                JOptionPane.showMessageDialog(TicketDetailPanel.this,
                        errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Cierra el ticket tras confirmación del usuario.
     */
    private void handleCloseTicket() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Confirmas que quieres cerrar este ticket?",
                "Cerrar ticket", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ticketService.closeTicket(ticket.getId(),
                    new TicketService.ServiceCallback<TicketDTO>() {
                @Override
                public void onSuccess(TicketDTO updated) {
                    ticket = updated;
                    onBack.run();
                }
                @Override
                public void onError(String errorMessage) {
                    JOptionPane.showMessageDialog(TicketDetailPanel.this,
                            errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    // ─── Helpers UI ───────────────────────────────────────────

    /**
     * Crea una etiqueta de título para el sidebar.
     *
     * @param text texto de la etiqueta
     * @return etiqueta con estilo de sidebar
     */
    private JLabel buildSidebarLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(160, 160, 160));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /**
     * Crea una etiqueta de valor para el sidebar.
     *
     * @param text texto del valor
     * @return etiqueta con estilo de valor
     */
    private JLabel buildSidebarValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return lbl;
    }

    /**
     * Crea el panel circular de color indicador de prioridad.
     *
     * @param priority nivel de prioridad
     * @return panel con el círculo de color
     */
    private JPanel buildPrioDot(String priority) {
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(priorityColor(priority));
                g2.fillOval(0, 0, 14, 14);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setMaximumSize(new Dimension(14, 14));
        return dot;
    }

    /**
     * Actualiza el color del punto de prioridad tras un cambio.
     *
     * @param newPriority nueva prioridad
     */
    private void updatePrioDot(String newPriority) {
        prioDot.putClientProperty("priority", newPriority);
        prioDot.repaint();
    }

    /**
     * Devuelve el color correspondiente a un nivel de prioridad.
     *
     * @param p nivel de prioridad
     * @return color asociado
     */
    private Color priorityColor(String p) {
        if (p == null) return PRIO_LOW;
        return switch (p) {
            case "HIGH"   -> PRIO_HIGH;
            case "MEDIUM" -> PRIO_MED;
            default       -> PRIO_LOW;
        };
    }

    /**
     * Construye un botón de navegación de paginación.
     *
     * @param label símbolo del botón
     * @return botón configurado
     */
    private JButton buildNavButton(String label) {
        JButton btn = new JButton(label);
        btn.setBackground(BG_DARK);
        btn.setForeground(new Color(160, 160, 160));
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(16f));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 80), 1, true),
                BorderFactory.createEmptyBorder(2, 14, 2, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Calcula el total de páginas para el hilo de comentarios.
     *
     * @return número de páginas (mínimo 1)
     */
    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) comments.size() / PAGE_SIZE));
    }

    /**
     * Formatea una fecha ISO-8601 a formato legible "dd/MM/yyyy HH:mm".
     *
     * @param iso fecha en formato ISO-8601
     * @return fecha formateada o el valor original si no se puede parsear
     */
    private String formatDate(String iso) {
        if (iso == null) return "";
        try {
            // Ejemplo: "2026-03-30T20:00:00" → "30/03/2026 20:00"
            String[] parts = iso.split("T");
            String[] date  = parts[0].split("-");
            String   time  = parts.length > 1 ? parts[1].substring(0, 5) : "";
            return date[2] + "/" + date[1] + "/" + date[0] + " " + time;
        } catch (Exception e) {
            return iso;
        }
    }

    /**
     * Muestra un mensaje de error en el panel cuando falla la carga inicial.
     *
     * @param message mensaje de error a mostrar
     */
    private void showError(String message) {
        removeAll();
        JLabel errorLabel = new JLabel(message, SwingConstants.CENTER);
        errorLabel.setForeground(new Color(200, 60, 60));
        add(errorLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

