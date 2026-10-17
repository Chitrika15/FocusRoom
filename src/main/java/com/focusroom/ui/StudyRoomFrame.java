package com.focusroom.ui;

import com.focusroom.model.RoomMember;
import com.focusroom.model.Student;
import com.focusroom.model.StudyResource;
import com.focusroom.model.StudyRoom;
import com.focusroom.model.StudySession;
import com.focusroom.service.StudyRoomService;
import com.focusroom.service.StudySessionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * Interactive Study Room View:
 * Features Shared Timer, Real-time Participant Tracker, Resource Sharing, and Room Controls.
 */
public class StudyRoomFrame extends JFrame {

    private final int roomId;
    private final Student currentStudent;
    private final DashboardFrame parentDashboard;

    private final StudyRoomService roomService;
    private final StudySessionService sessionService;

    // UI Components - Header
    private JLabel roomTitleLabel;
    private JLabel roomMetaLabel;
    private JLabel capacityBadge;
    private JLabel statusBadge;
    private JButton closeRoomBtn;

    // Timer & Session
    private JLabel timerDisplayLabel;
    private JLabel sessionStatusLabel;
    private JButton startSessionBtn;
    private JButton stopSessionBtn;
    private Timer uiTimer;               // updates elapsed seconds on UI
    private StudySession currentActiveSession;

    // Participants Table
    private DefaultTableModel membersTableModel;
    private JTable membersTable;

    // Resources Table
    private DefaultTableModel resourcesTableModel;
    private JTable resourcesTable;
    private JTextField resourceTitleField;
    private JTextField resourceLinkField;

    // Background Auto-Refresh Timer (every 10 seconds)
    private Timer autoSyncTimer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudyRoomFrame(int roomId, Student currentStudent, DashboardFrame parentDashboard) {
        this.roomId = roomId;
        this.currentStudent = currentStudent;
        this.parentDashboard = parentDashboard;
        this.roomService = new StudyRoomService();
        this.sessionService = new StudySessionService();

        initUI();
        loadAllRoomData();
        setupSyncTimers();
    }

    private void initUI() {
        setTitle("FocusRoom – Virtual Study Room #" + roomId);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        // 1. Top Room Header
        mainPanel.add(buildHeaderPanel(), BorderLayout.NORTH);

        // 2. Central Split Pane: Left (Timer + Resources), Right (Participants)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.68);
        splitPane.setBorder(null);

        // Left Panel: Timer Card & Resources Card
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        leftPanel.add(buildTimerCard());
        leftPanel.add(Box.createVerticalStrut(12));
        leftPanel.add(buildResourcesCard());

        // Right Panel: Participants Card
        JPanel rightPanel = buildParticipantsCard();

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Window closing hook to stop local timers
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopTimers();
                if (parentDashboard != null) {
                    parentDashboard.refreshAllData();
                }
            }
        });

        setContentPane(mainPanel);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = UITheme.createCardPanel();
        header.setLayout(new BorderLayout());

        // Left info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        roomTitleLabel = new JLabel("Loading Room Details...");
        roomTitleLabel.setFont(UITheme.FONT_TITLE);
        roomTitleLabel.setForeground(UITheme.TEXT_MAIN);

        roomMetaLabel = new JLabel("Subject: -- | Exam: -- | Creator: --");
        roomMetaLabel.setFont(UITheme.FONT_BODY);
        roomMetaLabel.setForeground(UITheme.TEXT_MUTED);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        badges.setOpaque(false);

        statusBadge = new JLabel(" [OPEN] ");
        statusBadge.setFont(UITheme.FONT_BODY_BOLD);
        statusBadge.setForeground(UITheme.ACCENT);

        capacityBadge = new JLabel(" Capacity: 0/0 ");
        capacityBadge.setFont(UITheme.FONT_BODY_BOLD);
        capacityBadge.setForeground(UITheme.PRIMARY);

        badges.add(statusBadge);
        badges.add(capacityBadge);

        info.add(roomTitleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(roomMetaLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(badges);

        // Right actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadAllRoomData());

        closeRoomBtn = UITheme.createDangerButton("Close Room");
        closeRoomBtn.setVisible(false); // only visible if creator
        closeRoomBtn.addActionListener(e -> performCloseRoom());

        JButton leaveBtn = UITheme.createSecondaryButton("🏃 Leave Room");
        leaveBtn.addActionListener(e -> performLeaveRoom());

        actions.add(refreshBtn);
        actions.add(closeRoomBtn);
        actions.add(leaveBtn);

        header.add(info, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildTimerCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JLabel title = new JLabel("⏱ Shared Study Session & Focus Timer");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.PRIMARY);
        top.add(title);

        // Center: Huge digital clock style timer
        JPanel timerCenter = new JPanel();
        timerCenter.setLayout(new BoxLayout(timerCenter, BoxLayout.Y_AXIS));
        timerCenter.setOpaque(false);

        timerDisplayLabel = new JLabel("00:00:00");
        timerDisplayLabel.setFont(UITheme.FONT_TIMER);
        timerDisplayLabel.setForeground(UITheme.TEXT_MAIN);
        timerDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sessionStatusLabel = new JLabel("No active study session in this room.");
        sessionStatusLabel.setFont(UITheme.FONT_BODY);
        sessionStatusLabel.setForeground(UITheme.TEXT_MUTED);
        sessionStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerCenter.add(timerDisplayLabel);
        timerCenter.add(Box.createVerticalStrut(6));
        timerCenter.add(sessionStatusLabel);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        controls.setOpaque(false);

        startSessionBtn = UITheme.createSuccessButton("▶ Start Focus Session");
        startSessionBtn.addActionListener(e -> performStartSession());

        stopSessionBtn = UITheme.createDangerButton("⏹ End Study Session");
        stopSessionBtn.setEnabled(false);
        stopSessionBtn.addActionListener(e -> performStopSession());

        controls.add(startSessionBtn);
        controls.add(stopSessionBtn);

        card.add(top, BorderLayout.NORTH);
        card.add(timerCenter, BorderLayout.CENTER);
        card.add(controls, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildResourcesCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        // Header
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JLabel title = new JLabel("📁 Shared Learning Resources & Materials");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.PRIMARY);
        top.add(title);

        // Table
        String[] columns = {"ID", "Resource Title", "Link / URL", "Shared By", "Shared At"};
        resourcesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        resourcesTable = new JTable(resourcesTableModel);
        resourcesTable.setRowHeight(26);
        resourcesTable.setFont(UITheme.FONT_BODY);
        resourcesTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        resourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resourcesTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        resourcesTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        resourcesTable.getColumnModel().getColumn(2).setPreferredWidth(220);

        JScrollPane scroll = new JScrollPane(resourcesTable);
        scroll.setPreferredSize(new Dimension(500, 160));

        // Add Resource Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 4, 3, 4);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.4;
        resourceTitleField = new JTextField();
        UITheme.styleTextField(resourceTitleField);
        form.add(resourceTitleField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(new JLabel("URL / Link:"), gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.4;
        resourceLinkField = new JTextField();
        UITheme.styleTextField(resourceLinkField);
        form.add(resourceLinkField, gbc);

        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0.0;
        JButton shareBtn = UITheme.createPrimaryButton("Share");
        shareBtn.addActionListener(e -> performShareResource());
        form.add(shareBtn, gbc);

        // Action: Open Selected Link
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        JButton openLinkBtn = UITheme.createSecondaryButton("🌐 Open Selected Link in Browser");
        openLinkBtn.addActionListener(e -> performOpenResourceLink());
        bottomBar.add(form, BorderLayout.CENTER);
        bottomBar.add(openLinkBtn, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(bottomBar, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildParticipantsCard() {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        JLabel title = new JLabel("👥 Active Room Members");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.PRIMARY);
        top.add(title);

        String[] columns = {"Name", "Dept", "Year", "Joined At"};
        membersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        membersTable = new JTable(membersTableModel);
        membersTable.setRowHeight(26);
        membersTable.setFont(UITheme.FONT_BODY);
        membersTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);

        JScrollPane scroll = new JScrollPane(membersTable);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void setupSyncTimers() {
        // Local clock timer: ticks every 1 second to update elapsed time if a session is running
        uiTimer = new Timer(1000, e -> updateElapsedDisplay());
        uiTimer.start();

        // Background sync timer: fetches latest DB updates every 8 seconds
        autoSyncTimer = new Timer(8000, e -> loadAllRoomData());
        autoSyncTimer.start();
    }

    private void stopTimers() {
        if (uiTimer != null) uiTimer.stop();
        if (autoSyncTimer != null) autoSyncTimer.stop();
    }

    public synchronized void loadAllRoomData() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Room Details
                Optional<StudyRoom> roomOpt = roomService.getRoomDetails(roomId);
                if (roomOpt.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Room no longer exists.", "Notice", JOptionPane.WARNING_MESSAGE);
                    dispose();
                    return;
                }

                StudyRoom room = roomOpt.get();
                roomTitleLabel.setText("🎯 " + room.getRoomName());
                roomMetaLabel.setText("Subject: " + room.getSubject() + "  |  Exam: " + room.getExamName() +
                        "  |  Host: " + (room.getCreatorName() != null ? room.getCreatorName() : "ID " + room.getCreatedBy()));

                capacityBadge.setText(" Capacity: " + room.getActiveParticipantCount() + " / " + room.getMaxParticipants() + " ");
                statusBadge.setText(" [" + room.getRoomStatus() + "] ");
                statusBadge.setForeground("OPEN".equalsIgnoreCase(room.getRoomStatus()) ? UITheme.ACCENT : UITheme.DANGER);

                // Enable Close button if current student is the creator and room is open
                boolean isCreator = room.getCreatedBy() == currentStudent.getStudentId();
                closeRoomBtn.setVisible(isCreator && "OPEN".equalsIgnoreCase(room.getRoomStatus()));

                // 2. Active Session
                Optional<StudySession> activeOpt = sessionService.getActiveSession(roomId);
                if (activeOpt.isPresent()) {
                    currentActiveSession = activeOpt.get();
                    startSessionBtn.setEnabled(false);
                    stopSessionBtn.setEnabled(true);
                    sessionStatusLabel.setText("🟢 Active Session in progress (Host: " +
                            (currentActiveSession.getStartedByName() != null ? currentActiveSession.getStartedByName() : "Member") + ")");
                    sessionStatusLabel.setForeground(UITheme.ACCENT);
                } else {
                    currentActiveSession = null;
                    startSessionBtn.setEnabled("OPEN".equalsIgnoreCase(room.getRoomStatus()));
                    stopSessionBtn.setEnabled(false);
                    timerDisplayLabel.setText("00:00:00");
                    sessionStatusLabel.setText("⚪ No active study session. Click 'Start Focus Session' to begin.");
                    sessionStatusLabel.setForeground(UITheme.TEXT_MUTED);
                }

                // 3. Members List
                List<RoomMember> members = roomService.getActiveMembers(roomId);
                membersTableModel.setRowCount(0);
                for (RoomMember m : members) {
                    membersTableModel.addRow(new Object[]{
                            m.getStudentName() + (m.getStudentId() == currentStudent.getStudentId() ? " (You)" : ""),
                            m.getDepartment(),
                            "Yr " + m.getYearOfStudy(),
                            m.getJoinedAt() != null ? dateFormat.format(m.getJoinedAt()) : "-"
                    });
                }

                // 4. Resources List
                List<StudyResource> resources = roomService.getResources(roomId);
                resourcesTableModel.setRowCount(0);
                for (StudyResource res : resources) {
                    resourcesTableModel.addRow(new Object[]{
                            res.getResourceId(),
                            res.getResourceTitle(),
                            res.getResourceLink(),
                            res.getStudentName() != null ? res.getStudentName() : "Student #" + res.getStudentId(),
                            res.getSharedAt() != null ? dateFormat.format(res.getSharedAt()) : "-"
                    });
                }

            } catch (Exception e) {
                // Silent fail or non-blocking during auto-sync
            }
        });
    }

    private void updateElapsedDisplay() {
        if (currentActiveSession != null && currentActiveSession.getStartTime() != null) {
            long startMs = currentActiveSession.getStartTime().getTime();
            long nowMs = System.currentTimeMillis();
            long elapsedSeconds = Math.max(0, (nowMs - startMs) / 1000);

            long hours = elapsedSeconds / 3600;
            long minutes = (elapsedSeconds % 3600) / 60;
            long seconds = elapsedSeconds % 60;

            timerDisplayLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    private void performStartSession() {
        try {
            StudySession session = sessionService.startSession(roomId, currentStudent.getStudentId());
            currentActiveSession = session;
            startSessionBtn.setEnabled(false);
            stopSessionBtn.setEnabled(true);
            JOptionPane.showMessageDialog(this, "Study session started! Focus timer is now ticking.",
                    "Session Started", JOptionPane.INFORMATION_MESSAGE);
            loadAllRoomData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot Start Session", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void performStopSession() {
        if (currentActiveSession == null) return;

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to conclude the current study session?",
                "Stop Session", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                StudySession completed = sessionService.stopSession(currentActiveSession.getSessionId(), currentStudent.getStudentId());
                currentActiveSession = null;
                timerDisplayLabel.setText("00:00:00");
                JOptionPane.showMessageDialog(this,
                        "Study session concluded!\nRecorded Duration: " + completed.getDurationMinutes() + " minute(s).",
                        "Session Completed", JOptionPane.INFORMATION_MESSAGE);
                loadAllRoomData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to stop session: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performShareResource() {
        String title = resourceTitleField.getText().trim();
        String link = resourceLinkField.getText().trim();

        if (title.isEmpty() || link.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both a title and a valid URL for the resource.",
                    "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            roomService.shareResource(roomId, currentStudent.getStudentId(), title, link);
            resourceTitleField.setText("");
            resourceLinkField.setText("");
            JOptionPane.showMessageDialog(this, "Resource shared successfully!", "Shared", JOptionPane.INFORMATION_MESSAGE);
            loadAllRoomData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Share Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void performOpenResourceLink() {
        int selectedRow = resourcesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a resource from the table to open its link.",
                    "No Resource Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String linkUrl = (String) resourcesTableModel.getValueAt(selectedRow, 2);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(linkUrl));
            } else {
                JOptionPane.showMessageDialog(this, "Browser launch not supported. Link: " + linkUrl,
                        "Link URL", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to open link: " + e.getMessage() + "\nURL: " + linkUrl,
                    "Link Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performLeaveRoom() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Do you want to leave this study room?\nYou can rejoin anytime from the dashboard if space is available.",
                "Leave Room", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                roomService.leaveRoom(roomId, currentStudent.getStudentId());
                stopTimers();
                dispose();
                if (parentDashboard != null) {
                    parentDashboard.refreshAllData();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error leaving room: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performCloseRoom() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently CLOSE this study room?\nNo new sessions or joins will be allowed.",
                "Confirm Close Room", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                roomService.closeRoom(roomId, currentStudent.getStudentId());
                JOptionPane.showMessageDialog(this, "Room closed successfully.");
                loadAllRoomData();
                if (parentDashboard != null) {
                    parentDashboard.refreshAllData();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to close room: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
