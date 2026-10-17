package com.focusroom.ui;

import com.focusroom.model.Student;
import com.focusroom.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Registration screen: Student Account Creation.
 */
public class RegisterFrame extends JFrame {

    private final AuthService authService;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> departmentCombo;
    private JComboBox<Integer> yearCombo;
    private JLabel statusLabel;

    public RegisterFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle("FocusRoom – Student Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Create Student Account");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join virtual study rooms and collaborate with peers");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(14));

        // Form Card
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // 1. Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        JLabel nameLbl = new JLabel("Full Name");
        nameLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(nameLbl, gbc);

        gbc.gridy = 1;
        nameField = new JTextField(20);
        UITheme.styleTextField(nameField);
        formCard.add(nameField, gbc);

        // 2. Email
        gbc.gridy = 2;
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(emailLbl, gbc);

        gbc.gridy = 3;
        emailField = new JTextField(20);
        UITheme.styleTextField(emailField);
        formCard.add(emailField, gbc);

        // 3. Password
        gbc.gridy = 4;
        JLabel passLbl = new JLabel("Password (min 6 characters)");
        passLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(passLbl, gbc);

        gbc.gridy = 5;
        passwordField = new JPasswordField(20);
        UITheme.styleTextField(passwordField);
        formCard.add(passwordField, gbc);

        // 4. Department
        gbc.gridy = 6;
        JLabel deptLbl = new JLabel("Department");
        deptLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(deptLbl, gbc);

        gbc.gridy = 7;
        String[] departments = {
                "Computer Science",
                "Information Technology",
                "Electronics & Communication",
                "Electrical & Electronics",
                "Mechanical Engineering",
                "Civil Engineering",
                "Data Science & AI",
                "Business Administration"
        };
        departmentCombo = new JComboBox<>(departments);
        departmentCombo.setFont(UITheme.FONT_BODY);
        formCard.add(departmentCombo, gbc);

        // 5. Year of Study
        gbc.gridy = 8;
        JLabel yearLbl = new JLabel("Year of Study");
        yearLbl.setFont(UITheme.FONT_BODY_BOLD);
        formCard.add(yearLbl, gbc);

        gbc.gridy = 9;
        Integer[] years = {1, 2, 3, 4, 5};
        yearCombo = new JComboBox<>(years);
        yearCombo.setFont(UITheme.FONT_BODY);
        formCard.add(yearCombo, gbc);

        // Status Label
        gbc.gridy = 10;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.DANGER);
        formCard.add(statusLabel, gbc);

        // Register Button
        gbc.gridy = 11;
        JButton registerBtn = UITheme.createPrimaryButton("Register Account");
        registerBtn.addActionListener(e -> performRegistration());
        formCard.add(registerBtn, gbc);

        // Back to Login Button
        gbc.gridy = 12;
        JButton backBtn = UITheme.createSecondaryButton("Back to Login");
        backBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        formCard.add(backBtn, gbc);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formCard, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private void performRegistration() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String department = (String) departmentCombo.getSelectedItem();
        Integer year = (Integer) yearCombo.getSelectedItem();

        statusLabel.setText("Processing registration...");
        statusLabel.setForeground(UITheme.PRIMARY);

        SwingUtilities.invokeLater(() -> {
            try {
                Student student = authService.register(name, email, password, department, year != null ? year : 1);
                JOptionPane.showMessageDialog(this,
                        "Registration Successful!\nWelcome, " + student.getStudentName() + ".\nPlease log in with your credentials.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                dispose();
                new LoginFrame().setVisible(true);

            } catch (IllegalArgumentException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.setForeground(UITheme.DANGER);
            } catch (Exception ex) {
                statusLabel.setText("Database error during registration.");
                statusLabel.setForeground(UITheme.DANGER);
                JOptionPane.showMessageDialog(this,
                        "Error saving student: " + ex.getMessage(),
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
