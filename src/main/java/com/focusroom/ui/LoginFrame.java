package com.focusroom.ui;

import com.focusroom.model.Student;
import com.focusroom.service.AuthService;
import com.focusroom.util.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Authentication screen: Student Login.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    public LoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle("FocusRoom – Student Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("🎯 FocusRoom");
        logoLabel.setFont(UITheme.FONT_TITLE);
        logoLabel.setForeground(UITheme.PRIMARY);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Virtual Study Rooms for Student Collaboration");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(logoLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(20));

        // Card with form fields
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Email Label & Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(emailLbl, gbc);

        gbc.gridy = 1;
        emailField = new JTextField(20);
        UITheme.styleTextField(emailField);
        formCard.add(emailField, gbc);

        // Password Label & Field
        gbc.gridy = 2;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(passLbl, gbc);

        gbc.gridy = 3;
        passwordField = new JPasswordField(20);
        UITheme.styleTextField(passwordField);
        formCard.add(passwordField, gbc);

        // Status / Error message label
        gbc.gridy = 4;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);
        formCard.add(statusLabel, gbc);

        // Sign In Button
        gbc.gridy = 5;
        JButton loginBtn = UITheme.createPrimaryButton("Sign In");
        loginBtn.addActionListener(e -> performLogin());
        formCard.add(loginBtn, gbc);

        // Divider or spacing
        gbc.gridy = 6;
        formCard.add(Box.createVerticalStrut(8), gbc);

        // Register Button
        gbc.gridy = 7;
        JButton registerBtn = UITheme.createSecondaryButton("Create New Account");
        registerBtn.addActionListener(e -> openRegisterFrame());
        formCard.add(registerBtn, gbc);

        // Trigger login on Enter key
        passwordField.addActionListener(e -> performLogin());
        emailField.addActionListener(e -> performLogin());

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);
        JLabel dbStatus = new JLabel(DBConnection.testConnection()
                ? "● Database Connected"
                : "○ Database Offline (Check db.properties)");
        dbStatus.setFont(UITheme.FONT_SMALL);
        dbStatus.setForeground(DBConnection.testConnection() ? UITheme.ACCENT : UITheme.DANGER);
        footerPanel.add(dbStatus);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formCard, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(UITheme.PRIMARY);

        SwingUtilities.invokeLater(() -> {
            try {
                Student student = authService.login(email, password);
                statusLabel.setText("Success! Loading dashboard...");
                statusLabel.setForeground(UITheme.ACCENT);

                // Open Dashboard
                dispose();
                new DashboardFrame(student).setVisible(true);

            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.setForeground(UITheme.DANGER);
            } catch (Exception ex) {
                statusLabel.setText("Database Connection Error. See details.");
                statusLabel.setForeground(UITheme.DANGER);
                JOptionPane.showMessageDialog(this,
                        "Failed to connect to MySQL database.\nPlease check db.properties and verify MySQL service.\n\nError: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openRegisterFrame() {
        dispose();
        new RegisterFrame().setVisible(true);
    }
}
