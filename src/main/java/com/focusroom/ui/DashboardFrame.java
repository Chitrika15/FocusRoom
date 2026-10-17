package com.focusroom.ui;

import com.focusroom.model.Student;
import com.focusroom.model.StudyRoom;
import com.focusroom.service.StudyRoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Main Student Dashboard: Room Discovery, My Rooms, and Room Creation.
 */
public class DashboardFrame extends JFrame {

    private final Student currentStudent;
    private final StudyRoomService roomService;

    // Available Rooms Table
    private DefaultTableModel availableRoomsModel;
    private JTable availableRoomsTable;
    private JTextField filterSubjectField;
    private JTextField filterExamField;

    // My Rooms Table
    private DefaultTableModel myRoomsModel;
    private JTable myRoomsTable;

    // Create Room Fields
    private JTextField createNameField;
    private JTextField createSubjectField;
    private JTextField createExamField;
    private JTextArea createDescArea;
    private JSpinner createCapacitySpinner;

    public DashboardFrame(Student student) {
        this.currentStudent = student;
        this.roomService = new StudyRoomService();
        initUI();
        refreshAllData();
    }

    private void initUI() {
        setTitle("FocusRoom – Student Dashboard (" + currentStudent.getStudentName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Navigation Bar
        mainPanel.add(buildTopBar(), BorderLayout.NORTH);

        // Center Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY_BOLD);
        tabbedPane.setBackground(UITheme.BG_LIGHT);

        tabbedPane.addTab("🔍 Browse & Join Rooms", buildBrowseRoomsPanel());
        tabbedPane.addTab("📚 My Active Rooms", buildMyRoomsPanel());
        tabbedPane.addTab("➕ Create Study Room", buildCreateRoomPanel());

        // Tab change listener to auto refresh
        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            if (selected == 0) loadAvailableRooms();
            else if (selected == 1) loadMyRooms();
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // Left: Branding and student info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel logo = new JLabel("🎯 FocusRoom Dashboard");
        logo.setFont(UITheme.FONT_HEADER);
        logo.setForeground(UITheme.PRIMARY);

        JLabel studentInfo = new JLabel("👤 " + currentStudent.getStudentName() + " (" +
                currentStudent.getEmail() + ") • " + currentStudent.getDepartment() + " (Year " + currentStudent.getYearOfStudy() + ")");
        studentInfo.setFont(UITheme.FONT_SMALL);
        studentInfo.setForeground(UITheme.TEXT_MUTED);

        infoPanel.add(logo);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(studentInfo);

        // Right: Actions (Study History, Logout)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton historyBtn = UITheme.createSecondaryButton("⏱ My Study History");
        historyBtn.addActionListener(e -> new StudyHistoryFrame(currentStudent).setVisible(true));

        JButton logoutBtn = UITheme.createDangerButton("Log Out");
        logoutBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        actionPanel.add(historyBtn);
        actionPanel.add(logoutBtn);

        bar.add(infoPanel, BorderLayout.WEST);
        bar.add(actionPanel, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBrowseRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Search & Filter Bar
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        filterCard.add(new JLabel("Subject:"));
        filterSubjectField = new JTextField(12);
        UITheme.styleTextField(filterSubjectField);
        filterCard.add(filterSubjectField);

        filterCard.add(new JLabel("Exam Name:"));
        filterExamField = new JTextField(12);
        UITheme.styleTextField(filterExamField);
        filterCard.add(filterExamField);

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> loadAvailableRooms());
        filterCard.add(searchBtn);

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            filterSubjectField.setText("");
            filterExamField.setText("");
            loadAvailableRooms();
        });
        filterCard.add(resetBtn);

        // Rooms Table
        String[] columns = {"ID", "Room Name", "Subject", "Exam Target", "Created By", "Capacity", "Live Session", "Status"};
        availableRoomsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        availableRoomsTable = new JTable(availableRoomsModel);
        availableRoomsTable.setRowHeight(28);
        availableRoomsTable.setFont(UITheme.FONT_BODY);
        availableRoomsTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        availableRoomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Center align numerical and status columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        availableRoomsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        availableRoomsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        availableRoomsTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        availableRoomsTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        availableRoomsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        availableRoomsTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        availableRoomsTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        availableRoomsTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(availableRoomsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));

        // Bottom Action Bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        actions.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadAvailableRooms());

        JButton joinBtn = UITheme.createSuccessButton("🤝 Join Selected Room");
        joinBtn.addActionListener(e -> performJoinRoom());

        JButton enterBtn = UITheme.createPrimaryButton("🚪 Enter Room");
        enterBtn.addActionListener(e -> performEnterSelectedAvailableRoom());

        actions.add(refreshBtn);
        actions.add(joinBtn);
        actions.add(enterBtn);

        panel.add(filterCard, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildMyRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));

        String[] columns = {"ID", "Room Name", "Subject", "Exam Target", "Created By", "Capacity", "Live Session", "Status"};
        myRoomsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        myRoomsTable = new JTable(myRoomsModel);
        myRoomsTable.setRowHeight(28);
        myRoomsTable.setFont(UITheme.FONT_BODY);
        myRoomsTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        myRoomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(myRoomsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        actions.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadMyRooms());

        JButton leaveBtn = UITheme.createDangerButton("Leave Room");
        leaveBtn.addActionListener(e -> performLeaveRoom());

        JButton enterBtn = UITheme.createPrimaryButton("🚪 Enter Study Room");
        enterBtn.addActionListener(e -> performEnterMyRoom());

        actions.add(refreshBtn);
        actions.add(leaveBtn);
        actions.add(enterBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCreateRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 40, 16, 40));

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Header
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel header = new JLabel("Start a New Virtual Study Room");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.PRIMARY);
        card.add(header, gbc);

        gbc.gridwidth = 1;

        // Room Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        card.add(new JLabel("Room Name:"), gbc);

        gbc.gridx = 1;
        createNameField = new JTextField(25);
        UITheme.styleTextField(createNameField);
        card.add(createNameField, gbc);

        // Subject
        gbc.gridx = 0;
        gbc.gridy = 2;
        card.add(new JLabel("Subject / Course:"), gbc);

        gbc.gridx = 1;
        createSubjectField = new JTextField(25);
        UITheme.styleTextField(createSubjectField);
        card.add(createSubjectField, gbc);

        // Exam Target
        gbc.gridx = 0;
        gbc.gridy = 3;
        card.add(new JLabel("Target Examination:"), gbc);

        gbc.gridx = 1;
        createExamField = new JTextField(25);
        UITheme.styleTextField(createExamField);
        card.add(createExamField, gbc);

        // Max Capacity
        gbc.gridx = 0;
        gbc.gridy = 4;
        card.add(new JLabel("Max Participants (2-50):"), gbc);

        gbc.gridx = 1;
        createCapacitySpinner = new JSpinner(new SpinnerNumberModel(8, 2, 50, 1));
        createCapacitySpinner.setFont(UITheme.FONT_BODY);
        card.add(createCapacitySpinner, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 5;
        card.add(new JLabel("Room Description:"), gbc);

        gbc.gridx = 1;
        createDescArea = new JTextArea(3, 25);
        createDescArea.setFont(UITheme.FONT_BODY);
        createDescArea.setLineWrap(true);
        createDescArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(createDescArea);
        card.add(descScroll, gbc);

        // Submit Button
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        JButton createBtn = UITheme.createPrimaryButton("Create Room & Enter");
        createBtn.addActionListener(e -> performCreateRoom());
        card.add(createBtn, gbc);

        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    public void refreshAllData() {
        loadAvailableRooms();
        loadMyRooms();
    }

    private void loadAvailableRooms() {
        String subject = filterSubjectField != null ? filterSubjectField.getText().trim() : "";
        String exam = filterExamField != null ? filterExamField.getText().trim() : "";

        availableRoomsModel.setRowCount(0);
        try {
            List<StudyRoom> rooms = roomService.getAvailableRooms(subject, exam);
            for (StudyRoom r : rooms) {
                availableRoomsModel.addRow(new Object[]{
                        r.getRoomId(),
                        r.getRoomName(),
                        r.getSubject(),
                        r.getExamName(),
                        r.getCreatorName() != null ? r.getCreatorName() : "ID " + r.getCreatedBy(),
                        r.getActiveParticipantCount() + " / " + r.getMaxParticipants(),
                        r.hasActiveSession() ? "🟢 Running" : "⚪ Idle",
                        r.getRoomStatus()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load open study rooms: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadMyRooms() {
        myRoomsModel.setRowCount(0);
        try {
            List<StudyRoom> rooms = roomService.getRoomsForStudent(currentStudent.getStudentId());
            for (StudyRoom r : rooms) {
                myRoomsModel.addRow(new Object[]{
                        r.getRoomId(),
                        r.getRoomName(),
                        r.getSubject(),
                        r.getExamName(),
                        r.getCreatorName() != null ? r.getCreatorName() : "ID " + r.getCreatedBy(),
                        r.getActiveParticipantCount() + " / " + r.getMaxParticipants(),
                        r.hasActiveSession() ? "🟢 Running" : "⚪ Idle",
                        r.getRoomStatus()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load your rooms: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performJoinRoom() {
        int selectedRow = availableRoomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to join from the table.",
                    "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = (int) availableRoomsModel.getValueAt(selectedRow, 0);
        String roomName = (String) availableRoomsModel.getValueAt(selectedRow, 1);

        try {
            boolean success = roomService.joinRoom(roomId, currentStudent.getStudentId());
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Successfully joined room: " + roomName,
                        "Joined", JOptionPane.INFORMATION_MESSAGE);
                refreshAllData();

                // Directly open the room
                new StudyRoomFrame(roomId, currentStudent, this).setVisible(true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Join Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void performEnterSelectedAvailableRoom() {
        int selectedRow = availableRoomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room from the table.",
                    "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int roomId = (int) availableRoomsModel.getValueAt(selectedRow, 0);

        try {
            if (!roomService.isStudentActiveMember(roomId, currentStudent.getStudentId())) {
                int option = JOptionPane.showConfirmDialog(this,
                        "You have not joined this room yet. Would you like to join now?",
                        "Join Room?", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    performJoinRoom();
                }
                return;
            }
            new StudyRoomFrame(roomId, currentStudent, this).setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performEnterMyRoom() {
        int selectedRow = myRoomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select one of your joined rooms to enter.",
                    "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int roomId = (int) myRoomsModel.getValueAt(selectedRow, 0);
        new StudyRoomFrame(roomId, currentStudent, this).setVisible(true);
    }

    private void performLeaveRoom() {
        int selectedRow = myRoomsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to leave.",
                    "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = (int) myRoomsModel.getValueAt(selectedRow, 0);
        String roomName = (String) myRoomsModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to leave '" + roomName + "'?\nYou can rejoin later if capacity is available.",
                "Leave Room Confirmation", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean left = roomService.leaveRoom(roomId, currentStudent.getStudentId());
                if (left) {
                    JOptionPane.showMessageDialog(this, "You have left the room.");
                    refreshAllData();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error leaving room: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performCreateRoom() {
        String name = createNameField.getText().trim();
        String subject = createSubjectField.getText().trim();
        String exam = createExamField.getText().trim();
        String desc = createDescArea.getText().trim();
        int capacity = (Integer) createCapacitySpinner.getValue();

        try {
            StudyRoom newRoom = roomService.createRoom(name, subject, exam, desc, capacity, currentStudent.getStudentId());
            JOptionPane.showMessageDialog(this,
                    "Study Room '" + newRoom.getRoomName() + "' created successfully!\nYou have been automatically enrolled.",
                    "Room Created", JOptionPane.INFORMATION_MESSAGE);

            // Clear form
            createNameField.setText("");
            createSubjectField.setText("");
            createExamField.setText("");
            createDescArea.setText("");
            createCapacitySpinner.setValue(8);

            refreshAllData();

            // Open the new room
            new StudyRoomFrame(newRoom.getRoomId(), currentStudent, this).setVisible(true);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to create room: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
