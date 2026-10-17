package com.focusroom.dao;

import com.focusroom.model.StudySession;
import com.focusroom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for STUDY_SESSIONS table.
 */
public class StudySessionDAO {

    /**
     * Starts a new study session in the room.
     * Prevents multiple active sessions using transactional checks.
     *
     * @param roomId    ID of the room
     * @param startedBy ID of the student starting the session
     * @return created StudySession
     * @throws SQLException if an active session already exists or database error occurs
     */
    public StudySession startSession(int roomId, int startedBy) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Check if there is already an active session in the room
            String checkActiveSql = "SELECT session_id FROM STUDY_SESSIONS WHERE room_id = ? AND session_status = 'ACTIVE'";
            try (PreparedStatement psCheck = conn.prepareStatement(checkActiveSql)) {
                psCheck.setInt(1, roomId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        throw new SQLException("An active study session is already running in this room.");
                    }
                }
            }

            // 2. Insert new session
            String insertSql = "INSERT INTO STUDY_SESSIONS (room_id, started_by, start_time, session_status) " +
                              "VALUES (?, ?, CURRENT_TIMESTAMP, 'ACTIVE')";
            StudySession session = new StudySession(roomId, startedBy);

            try (PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                psInsert.setInt(1, roomId);
                psInsert.setInt(2, startedBy);
                psInsert.executeUpdate();

                try (ResultSet rs = psInsert.getGeneratedKeys()) {
                    if (rs.next()) {
                        session.setSessionId(rs.getInt(1));
                    }
                }
            }

            // Retrieve generated start time
            String fetchSql = "SELECT start_time FROM STUDY_SESSIONS WHERE session_id = ?";
            try (PreparedStatement psFetch = conn.prepareStatement(fetchSql)) {
                psFetch.setInt(1, session.getSessionId());
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        session.setStartTime(rs.getTimestamp("start_time"));
                    }
                }
            }

            conn.commit();
            return session;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Stops an active study session, calculates the duration accurately from database timestamps,
     * and sets status to COMPLETED.
     *
     * @param sessionId the session identifier
     * @return updated StudySession
     * @throws SQLException if session not found or already completed
     */
    public StudySession stopSession(int sessionId) throws SQLException {
        String fetchSql = "SELECT * FROM STUDY_SESSIONS WHERE session_id = ?";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBConnection.getConnection()) {
            Timestamp startTime = null;
            String status = null;

            try (PreparedStatement psFetch = conn.prepareStatement(fetchSql)) {
                psFetch.setInt(1, sessionId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        startTime = rs.getTimestamp("start_time");
                        status = rs.getString("session_status");
                    } else {
                        throw new SQLException("Session not found: ID " + sessionId);
                    }
                }
            }

            if (!"ACTIVE".equalsIgnoreCase(status)) {
                throw new SQLException("Session is not active (current status: " + status + ").");
            }

            // Calculate duration in minutes (minimum 1 minute if ran for at least a few seconds)
            long diffMillis = Math.max(0, now.getTime() - startTime.getTime());
            int durationMinutes = (int) Math.round(diffMillis / 60000.0);
            if (durationMinutes == 0 && diffMillis >= 5000) {
                // If it ran for >= 5 seconds, count as at least 1 min for credit
                durationMinutes = 1;
            }

            String updateSql = "UPDATE STUDY_SESSIONS SET end_time = ?, duration_minutes = ?, session_status = 'COMPLETED' " +
                              "WHERE session_id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setTimestamp(1, now);
                psUpdate.setInt(2, durationMinutes);
                psUpdate.setInt(3, sessionId);
                psUpdate.executeUpdate();
            }

            StudySession s = new StudySession();
            s.setSessionId(sessionId);
            s.setStartTime(startTime);
            s.setEndTime(now);
            s.setDurationMinutes(durationMinutes);
            s.setSessionStatus("COMPLETED");
            return s;
        }
    }

    /**
     * Gets the currently active study session for a room, if any.
     */
    public Optional<StudySession> getActiveSession(int roomId) throws SQLException {
        String sql = "SELECT ss.*, sr.room_name, sr.subject, s.student_name AS started_by_name " +
                     "FROM STUDY_SESSIONS ss " +
                     "JOIN STUDY_ROOMS sr ON ss.room_id = sr.room_id " +
                     "JOIN STUDENTS s ON ss.started_by = s.student_id " +
                     "WHERE ss.room_id = ? AND ss.session_status = 'ACTIVE' " +
                     "LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSession(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves study history initiated by or participated in by a student.
     */
    public List<StudySession> getSessionsByStudent(int studentId) throws SQLException {
        // Sessions started by the student or conducted in rooms the student is/was a member of
        String sql = "SELECT DISTINCT ss.*, sr.room_name, sr.subject, s.student_name AS started_by_name " +
                     "FROM STUDY_SESSIONS ss " +
                     "JOIN STUDY_ROOMS sr ON ss.room_id = sr.room_id " +
                     "JOIN STUDENTS s ON ss.started_by = s.student_id " +
                     "LEFT JOIN ROOM_MEMBERS rm ON rm.room_id = ss.room_id AND rm.student_id = ? " +
                     "WHERE (ss.started_by = ? OR rm.student_id = ?) " +
                     "ORDER BY ss.start_time DESC";

        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        }
        return sessions;
    }

    /**
     * Retrieves study sessions for a specific room.
     */
    public List<StudySession> getSessionsByRoom(int roomId) throws SQLException {
        String sql = "SELECT ss.*, sr.room_name, sr.subject, s.student_name AS started_by_name " +
                     "FROM STUDY_SESSIONS ss " +
                     "JOIN STUDY_ROOMS sr ON ss.room_id = sr.room_id " +
                     "JOIN STUDENTS s ON ss.started_by = s.student_id " +
                     "WHERE ss.room_id = ? " +
                     "ORDER BY ss.start_time DESC";

        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        }
        return sessions;
    }

    /**
     * Calculates the sum total of study minutes for a student across all completed sessions.
     */
    public int getTotalStudyMinutesForStudent(int studentId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) " +
                     "FROM STUDY_SESSIONS " +
                     "WHERE started_by = ? AND session_status = 'COMPLETED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private StudySession mapResultSetToSession(ResultSet rs) throws SQLException {
        StudySession s = new StudySession();
        s.setSessionId(rs.getInt("session_id"));
        s.setRoomId(rs.getInt("room_id"));
        s.setStartedBy(rs.getInt("started_by"));
        s.setStartTime(rs.getTimestamp("start_time"));
        s.setEndTime(rs.getTimestamp("end_time"));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        s.setSessionStatus(rs.getString("session_status"));

        try {
            s.setRoomName(rs.getString("room_name"));
        } catch (SQLException ignored) {}

        try {
            s.setSubject(rs.getString("subject"));
        } catch (SQLException ignored) {}

        try {
            s.setStartedByName(rs.getString("started_by_name"));
        } catch (SQLException ignored) {}

        return s;
    }
}
