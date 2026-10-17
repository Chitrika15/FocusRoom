package com.focusroom.ui;

import com.focusroom.model.Student;
import com.focusroom.model.StudySession;
import com.focusroom.service.StudySessionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Study History Screen:
 * Displays student study sessions, total hours/minutes studied, and breakdown.
 */
public class StudyHistoryFrame extends JFrame {

    private final Student currentStudent;
    private final StudySessionService sessionService;

    private DefaultTableModel historyTableModel;
    private JTable historyTable;
    private JLabel totalTimeCardLabel;
    private JLabel totalSessionsCardLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudyHistoryFrame(Student currentStudent) {
        this.currentStudent = currentStudent;
        this.sessionService = new StudySessionService();
        initUI();
        loadHistoryData();
    }

    private void initUI() {
        setTitle("FocusRoom – Study History (" + currentStudent.getStudentName() + ")");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(880, 560);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Top Summary Banner
        mainPanel.add(buildSummaryBanner(), BorderLayout.NORTH);

        // 2. Table of Study Sessions
        String[] columns = {"Session ID", "Room Name", "Subject", "Started By", "Start Time", "End Time", "Duration (mins)", "Status"};
        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        historyTable.setRowHeight(26);
        historyTable.setFont(UITheme.FONT_BODY);
        historyTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. Footer Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        footer.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadHistoryData());

        JButton closeBtn = UITheme.createPrimaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        footer.add(refreshBtn);
        footer.add(closeBtn);
        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildSummaryBanner() {
        JPanel banner = new JPanel(new GridLayout(1, 2, 14, 0));
        banner.setOpaque(false);

        // Card 1: Total Study Time
        JPanel timeCard = UITheme.createCardPanel();
        timeCard.setLayout(new BoxLayout(timeCard, BoxLayout.Y_AXIS));

        JLabel titleTime = new JLabel("⏳ Total Focus Study Time");
        titleTime.setFont(UITheme.FONT_SUBHEADER);
        titleTime.setForeground(UITheme.TEXT_MUTED);

        totalTimeCardLabel = new JLabel("0 Hours 0 Mins");
        totalTimeCardLabel.setFont(UITheme.FONT_TITLE);
        totalTimeCardLabel.setForeground(UITheme.PRIMARY);

        timeCard.add(titleTime);
        timeCard.add(Box.createVerticalStrut(4));
        timeCard.add(totalTimeCardLabel);

        // Card 2: Total Sessions Completed
        JPanel sessionCard = UITheme.createCardPanel();
        sessionCard.setLayout(new BoxLayout(sessionCard, BoxLayout.Y_AXIS));

        JLabel titleSession = new JLabel("🎯 Completed Sessions");
        titleSession.setFont(UITheme.FONT_SUBHEADER);
        titleSession.setForeground(UITheme.TEXT_MUTED);

        totalSessionsCardLabel = new JLabel("0 Sessions");
        totalSessionsCardLabel.setFont(UITheme.FONT_TITLE);
        totalSessionsCardLabel.setForeground(UITheme.ACCENT);

        sessionCard.add(titleSession);
        sessionCard.add(Box.createVerticalStrut(4));
        sessionCard.add(totalSessionsCardLabel);

        banner.add(timeCard);
        banner.add(sessionCard);
        return banner;
    }

    private void loadHistoryData() {
        historyTableModel.setRowCount(0);
        try {
            int totalMins = sessionService.getTotalStudyMinutes(currentStudent.getStudentId());
            int hours = totalMins / 60;
            int minutes = totalMins % 60;
            totalTimeCardLabel.setText(hours + " hr " + minutes + " min (" + totalMins + " mins)");

            List<StudySession> sessions = sessionService.getStudentHistory(currentStudent.getStudentId());
            int completedCount = 0;

            for (StudySession s : sessions) {
                if ("COMPLETED".equalsIgnoreCase(s.getSessionStatus())) {
                    completedCount++;
                }

                String startedByStr = (s.getStartedBy() == currentStudent.getStudentId())
                        ? "You"
                        : (s.getStartedByName() != null ? s.getStartedByName() : "Student #" + s.getStartedBy());

                historyTableModel.addRow(new Object[]{
                        s.getSessionId(),
                        s.getRoomName() != null ? s.getRoomName() : "Room #" + s.getRoomId(),
                        s.getSubject() != null ? s.getSubject() : "--",
                        startedByStr,
                        s.getStartTime() != null ? dateFormat.format(s.getStartTime()) : "-",
                        s.getEndTime() != null ? dateFormat.format(s.getEndTime()) : "--",
                        s.getDurationMinutes() + " min",
                        s.getSessionStatus()
                });
            }

            totalSessionsCardLabel.setText(completedCount + " Sessions (" + sessions.size() + " Total)");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load study history: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
