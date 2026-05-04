package com.ticketmaster.ticketplusclient.gui;

import com.ticketmaster.ticketplusclient.model.UserDTO;
import com.ticketmaster.ticketplusclient.session.SessionManager;
import com.ticketmaster.ticketplusclient.session.UserService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author eriks
 */
public class UserManagementPanel extends JPanel {

    private static final Color BG_MID     = new Color(34,  40,  44);
    private static final Color BG_CARD    = Color.WHITE;
    private static final Color TEXT_DARK  = new Color(30,  30,  30);
    private static final Color TEXT_MUTED = new Color(120, 120, 120);
    private static final Color ACCENT     = new Color(54,  81,  207);
    private static final Color BTN_GRAY   = new Color(100, 110, 120);
    private static final Color BTN_BACK   = new Color(70,  80,  90);
    private static final Color BTN_DANGER = new Color(180, 60,  60);

    private final UserService userService;
    private final Runnable onBack;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton createBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;
    private JButton backBtn;

    public UserManagementPanel(Runnable onBack) {
        this.userService = new UserService();
        this.onBack = onBack;

        setLayout(new BorderLayout());
        setBackground(BG_MID);

        add(buildCard(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildCard() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_MID);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        card.add(buildCardHeader(), BorderLayout.NORTH);
        card.add(buildCardBody(), BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        wrapper.add(card, gbc);

        return wrapper;
    }

    private JPanel buildCardHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setBackground(BG_CARD);

        JLabel title = new JLabel("User management");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Create and delete users");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        left.add(title);
        left.add(subtitle);

        String username = SessionManager.getInstance().getUsername();
        JLabel byLabel = new JLabel("By: " + username);
        byLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        byLabel.setForeground(TEXT_MUTED);

        header.add(left, BorderLayout.WEST);
        header.add(byLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCardBody() {
        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setBackground(BG_CARD);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        body.add(buildFormPanel(), BorderLayout.WEST);
        body.add(buildUsersTablePanel(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel();
        form.setBackground(BG_CARD);
        form.setPreferredSize(new Dimension(260, 0));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel formTitle = new JLabel("New user");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        formTitle.setForeground(TEXT_DARK);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = buildTextField();
        passwordField = buildPasswordField();

        roleCombo = new JComboBox<>(new String[]{"USER", "ADMIN"});
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        roleCombo.setBackground(BG_CARD);
        roleCombo.setForeground(TEXT_DARK);
        roleCombo.setFocusable(false);
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        createBtn = buildButton("Create user", ACCENT);
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createBtn.addActionListener(e -> handleCreateUser());

        form.add(formTitle);
        form.add(Box.createRigidArea(new Dimension(0, 16)));

        form.add(buildLabel("Username"));
        form.add(usernameField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(buildLabel("Password"));
        form.add(passwordField);
        form.add(Box.createRigidArea(new Dimension(0, 12)));

        form.add(buildLabel("Role"));
        form.add(roleCombo);
        form.add(Box.createRigidArea(new Dimension(0, 18)));

        form.add(createBtn);
        form.add(Box.createVerticalGlue());

        return form;
    }

    private JPanel buildUsersTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_CARD);

        JLabel listTitle = new JLabel("Active users");
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        listTitle.setForeground(TEXT_DARK);

        tableModel = new DefaultTableModel(new Object[]{"Username", "Role", "Active"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        userTable.setRowHeight(30);
        userTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(240, 240, 240));
        userTable.getTableHeader().setForeground(TEXT_DARK);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        panel.add(listTitle, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_MID);

        backBtn = buildButton("Back", BTN_BACK);
        backBtn.addActionListener(e -> onBack.run());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        left.setBackground(BG_MID);
        left.add(backBtn);

        refreshBtn = buildButton("Refresh", BTN_GRAY);
        refreshBtn.addActionListener(e -> refresh());

        deleteBtn = buildButton("Delete selected", BTN_DANGER);
        deleteBtn.addActionListener(e -> handleDeleteUser());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        right.setBackground(BG_MID);
        right.add(refreshBtn);
        right.add(deleteBtn);

        outer.add(left, BorderLayout.WEST);
        outer.add(right, BorderLayout.EAST);
        return outer;
    }

    private JLabel buildLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(TEXT_DARK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField buildTextField() {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT_DARK);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        return field;
    }

    private JPasswordField buildPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT_DARK);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        return field;
    }

    private JButton buildButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        return button;
    }

    public void refresh() {
        setControlsEnabled(false);

        userService.getUsers(new UserService.ServiceCallback<List<UserDTO>>() {
            @Override
            public void onSuccess(List<UserDTO> result) {
                tableModel.setRowCount(0);

                for (UserDTO user : result) {
                    tableModel.addRow(new Object[]{
                            user.getUsername(),
                            user.getRole(),
                            user.isActive() ? "Yes" : "No"
                    });
                }

                setControlsEnabled(true);
            }

            @Override
            public void onError(String errorMessage) {
                setControlsEnabled(true);
                JOptionPane.showMessageDialog(
                        UserManagementPanel.this,
                        errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void handleCreateUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = String.valueOf(roleCombo.getSelectedItem());

        if (username.isBlank()) {
            JOptionPane.showMessageDialog(this, "Username is required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Password is required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setControlsEnabled(false);

        userService.createUser(username, password, role, new UserService.ServiceCallback<UserDTO>() {
            @Override
            public void onSuccess(UserDTO result) {
                usernameField.setText("");
                passwordField.setText("");
                roleCombo.setSelectedItem("USER");

                JOptionPane.showMessageDialog(
                        UserManagementPanel.this,
                        "User created: " + result.getUsername(),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                refresh();
            }

            @Override
            public void onError(String errorMessage) {
                setControlsEnabled(true);
                JOptionPane.showMessageDialog(
                        UserManagementPanel.this,
                        errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void handleDeleteUser() {
        int selectedRow = userTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a user first", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = String.valueOf(tableModel.getValueAt(selectedRow, 0));
        String currentUsername = SessionManager.getInstance().getUsername();

        if (username.equalsIgnoreCase(currentUsername)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own user", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete user '" + username + "'?",
                "Confirm delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setControlsEnabled(false);

        userService.deleteUser(username, new UserService.ServiceCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                JOptionPane.showMessageDialog(
                        UserManagementPanel.this,
                        "User deleted: " + username,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                refresh();
            }

            @Override
            public void onError(String errorMessage) {
                setControlsEnabled(true);
                JOptionPane.showMessageDialog(
                        UserManagementPanel.this,
                        errorMessage,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        if (usernameField != null) usernameField.setEnabled(enabled);
        if (passwordField != null) passwordField.setEnabled(enabled);
        if (roleCombo != null) roleCombo.setEnabled(enabled);
        if (createBtn != null) createBtn.setEnabled(enabled);
        if (deleteBtn != null) deleteBtn.setEnabled(enabled);
        if (refreshBtn != null) refreshBtn.setEnabled(enabled);
        if (backBtn != null) backBtn.setEnabled(enabled);
        if (userTable != null) userTable.setEnabled(enabled);
    }
}