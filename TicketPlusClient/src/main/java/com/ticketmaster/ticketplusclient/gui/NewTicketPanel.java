/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.TicketDTO;
import com.ticketmaster.ticketplusclient.session.TicketService;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.MatteBorder;

/**
 *
 * @author Christian G
 */
public class NewTicketPanel extends JPanel {

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

    private final TicketService ticketService;
    private final Runnable      onSaved;

    // ─── Componentes ──────────────────────────────────────────
    private JTextField  titleField;
    private JTextArea   descArea;
    private JComboBox<String> priorityCombo;
    private JButton     saveBtn;
    private JButton     cancelBtn;

    /**
     * Crea el panel de nuevo ticket.
     *
     * @param onSaved  acción a ejecutar tras guardar correctamente (refresca la lista)
     * @param onCancel acción a ejecutar al cancelar (vuelve a la lista)
     */
    public NewTicketPanel(Runnable onSaved, Runnable onCancel) {
        this.ticketService = new TicketService();
        this.onSaved       = onSaved;

        setLayout(new BorderLayout());
        setBackground(BG_MID);

        add(buildCard(), BorderLayout.CENTER);
        add(buildBottomBar(onCancel), BorderLayout.SOUTH);
    }

    // ─── Construcción UI ──────────────────────────────────────

    /**
     * Construye la tarjeta central blanca con el formulario.
     *
     * @return panel con el formulario de creación
     */
    private JPanel buildCard() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_MID);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        card.add(buildCardHeader(), BorderLayout.NORTH);
        card.add(buildCardBody(),   BorderLayout.CENTER);
        card.add(buildCardFooter(), BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        wrapper.add(card, gbc);

        return wrapper;
    }

    /**
     * Cabecera de la tarjeta: campo título a la izquierda y "By: usuario" a la derecha.
     *
     * @return panel de cabecera
     */
    private JPanel buildCardHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        titleField = new JTextField();
        titleField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        titleField.setBorder(BorderFactory.createEmptyBorder());
        titleField.setBackground(BG_CARD);
        titleField.setForeground(TEXT_DARK);
        titleField.putClientProperty("JTextField.placeholderText", "Title: ...");

        // Placeholder manual para entornos sin soporte de putClientProperty
        titleField.setText("Title: ...");
        titleField.setForeground(TEXT_MUTED);
        titleField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if ("Title: ...".equals(titleField.getText())) {
                    titleField.setText("");
                    titleField.setForeground(TEXT_DARK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (titleField.getText().isEmpty()) {
                    titleField.setText("Title: ...");
                    titleField.setForeground(TEXT_MUTED);
                }
            }
        });

        String username = SessionManager.getInstance().getUsername();
        JLabel byLabel = new JLabel("By: " + username);
        byLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        byLabel.setForeground(TEXT_MUTED);

        header.add(titleField, BorderLayout.CENTER);
        header.add(byLabel,    BorderLayout.EAST);
        return header;
    }

    /**
     * Cuerpo de la tarjeta: etiqueta "Description" y área de texto.
     *
     * @return panel del cuerpo
     */
    private JPanel buildCardBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(BG_CARD);
        body.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel descLabel = new JLabel("Description");
        descLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        descLabel.setForeground(TEXT_DARK);

        descArea = new JTextArea();
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        descArea.setBackground(BG_CARD);
        descArea.setForeground(TEXT_DARK);

        JScrollPane scroll = new JScrollPane(descArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        body.add(descLabel, BorderLayout.NORTH);
        body.add(scroll,    BorderLayout.CENTER);
        return body;
    }

    /**
     * Pie de la tarjeta: botones Save y Cancel a la derecha.
     *
     * @return panel del pie
     */
    private JPanel buildCardFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.setBackground(BG_CARD);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        saveBtn = new JButton("Save");
        saveBtn.setBackground(new Color(100, 110, 120));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> handleSave());

        cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(100, 110, 120));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        footer.add(saveBtn);
        footer.add(cancelBtn);
        return footer;
    }

    /**
     * Barra inferior derecha con el selector de prioridad.
     *
     * @param onCancel acción del botón Back/Cancel
     * @return panel lateral de prioridad
     */
    private JPanel buildBottomBar(Runnable onCancel) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_MID);

        // Botón back izquierda
        JButton backBtn = new JButton("Back");
        backBtn.setBackground(new Color(70, 80, 90));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        backBtn.addActionListener(e -> onCancel.run());

        JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bottomLeft.setBackground(BG_MID);
        bottomLeft.add(backBtn);

        // Acción cancel también vuelve
        cancelBtn.addActionListener(e -> onCancel.run());

        outer.add(bottomLeft, BorderLayout.WEST);
        outer.add(buildPrioritySidebar(), BorderLayout.EAST);
        return outer;
    }

    /**
     * Panel lateral derecho con el selector de prioridad.
     *
     * @return panel con el combo de prioridad
     */
    private JPanel buildPrioritySidebar() {
        JPanel sidebar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        sidebar.setBackground(BG_MID);

        JLabel prioLabel = new JLabel("Priority:");
        prioLabel.setForeground(Color.WHITE);
        prioLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        priorityCombo = new JComboBox<>(new String[]{"HIGH", "MEDIUM", "LOW"});
        priorityCombo.setSelectedItem("MEDIUM");
        priorityCombo.setBackground(new Color(70, 80, 90));
        priorityCombo.setForeground(Color.WHITE);
        priorityCombo.setFocusable(false);

        sidebar.add(prioLabel);
        sidebar.add(priorityCombo);
        return sidebar;
    }

    // ─── Lógica ───────────────────────────────────────────────

    /**
     * Valida los campos y envía la petición de creación al servidor.
     * Si tiene éxito ejecuta {@link #onSaved} para refrescar la lista.
     */
    private void handleSave() {
        String title    = titleField.getText().trim();
        String desc     = descArea.getText().trim();
        String priority = (String) priorityCombo.getSelectedItem();

        if (title.isEmpty() || "Title: ...".equals(title)) {
            JOptionPane.showMessageDialog(this, "El título es obligatorio.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La descripción es obligatoria.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            descArea.requestFocus();
            return;
        }

        saveBtn.setEnabled(false);
        saveBtn.setText("Guardando...");

        ticketService.createTicket(title, desc, priority,
                new TicketService.ServiceCallback<TicketDTO>() {
            @Override
            public void onSuccess(TicketDTO ticket) {
                saveBtn.setEnabled(true);
                saveBtn.setText("Save");
                JOptionPane.showMessageDialog(NewTicketPanel.this,
                        "Ticket " + ticket.getRef() + " creado correctamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                onSaved.run();
            }
            @Override
            public void onError(String errorMessage) {
                saveBtn.setEnabled(true);
                saveBtn.setText("Save");
                JOptionPane.showMessageDialog(NewTicketPanel.this,
                        errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    /**
    * Limpia todos los campos del formulario para permitir crear un nuevo ticket
    * sin residuos de la entrada anterior.
    */
    public void reset() {
        titleField.setText("Title: ...");
        titleField.setForeground(TEXT_MUTED);
        descArea.setText("");
        priorityCombo.setSelectedItem("MEDIUM");
        saveBtn.setEnabled(true);
        saveBtn.setText("Save");
    }
}

