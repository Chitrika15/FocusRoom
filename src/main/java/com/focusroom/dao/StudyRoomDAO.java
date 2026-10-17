package com.focusroom.dao;

import com.focusroom.model.StudyRoom;
import com.focusroom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for STUDY_ROOMS table.
 */
public class StudyRoomDAO {

    /**
     * Creates a new study room and automatically enrolls the creator as an ACTIVE member.
     * Uses a database transaction so both records succeed together.
     *
     * @param room StudyRoom object
     * @return true if created successfully
     * @throws SQLException if a database error occurs
     */
    public boolean createRoom(StudyRoom room) throws SQLException {
        String insertRoomSql = "INSERT INTO STUDY_ROOMS (room_name, subject, exam_name, created_by, room_description, max_participants, room_status) " +
                               "VALUES (?, ?, ?, ?, ?, ?, 'OPEN')";
        String insertMemberSql = "INSERT INTO ROOM_MEMBERS (room_id, student_id, joined_at, participant_status) VALUES (?, ?, CURRENT_TIMESTAMP, 'ACTIVE')";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            try (PreparedStatement psRoom = conn.prepareStatement(insertRoomSql, Statement.RETURN_GENERATED_KEYS)) {
                psRoom.setString(1, room.getRoomName());
                psRoom.setString(2, room.getSubject());
                psRoom.setString(3, room.getExamName());
                psRoom.setInt(4, room.getCreatedBy());
                psRoom.setString(5, room.getRoomDescription());
                psRoom.setInt(6, room.getMaxParticipants());

                int affected = psRoom.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet rs = psRoom.getGeneratedKeys()) {
                    if (rs.next()) {
                        room.setRoomId(rs.getInt(1));
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Enroll creator in ROOM_MEMBERS
            try (PreparedStatement psMember = conn.prepareStatement(insertMemberSql)) {
                psMember.setInt(1, room.getRoomId());
                psMember.setInt(2, room.getCreatedBy());
                psMember.executeUpdate();
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // Suppress
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Suppress
                }
            }
        }
    }

    /**
     * Finds a room by ID with creator details and active counts.
     */
    public Optional<StudyRoom> findById(int roomId) throws SQLException {
        String sql = "SELECT r.*, s.student_name AS creator_name, " +
                     "(SELECT COUNT(*) FROM ROOM_MEMBERS rm WHERE rm.room_id = r.room_id AND rm.participant_status = 'ACTIVE') AS active_count, " +
                     "(SELECT COUNT(*) FROM STUDY_SESSIONS ss WHERE ss.room_id = r.room_id AND ss.session_status = 'ACTIVE') AS active_session_count " +
                     "FROM STUDY_ROOMS r " +
                     "JOIN STUDENTS s ON r.created_by = s.student_id " +
                     "WHERE r.room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRoom(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all OPEN rooms matching optional subject and examination filters.
     */
    public List<StudyRoom> findOpenRooms(String subjectFilter, String examFilter) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.*, s.student_name AS creator_name, ");
        sql.append("(SELECT COUNT(*) FROM ROOM_MEMBERS rm WHERE rm.room_id = r.room_id AND rm.participant_status = 'ACTIVE') AS active_count, ");
        sql.append("(SELECT COUNT(*) FROM STUDY_SESSIONS ss WHERE ss.room_id = r.room_id AND ss.session_status = 'ACTIVE') AS active_session_count ");
        sql.append("FROM STUDY_ROOMS r ");
        sql.append("JOIN STUDENTS s ON r.created_by = s.student_id ");
        sql.append("WHERE r.room_status = 'OPEN' ");

        List<String> params = new ArrayList<>();
        if (subjectFilter != null && !subjectFilter.trim().isEmpty()) {
            sql.append("AND LOWER(r.subject) LIKE ? ");
            params.add("%" + subjectFilter.trim().toLowerCase() + "%");
        }
        if (examFilter != null && !examFilter.trim().isEmpty()) {
            sql.append("AND LOWER(r.exam_name) LIKE ? ");
            params.add("%" + examFilter.trim().toLowerCase() + "%");
        }
        sql.append("ORDER BY r.created_at DESC");

        List<StudyRoom> rooms = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapResultSetToRoom(rs));
                }
            }
        }
        return rooms;
    }

    /**
     * Finds rooms where the specified student is an ACTIVE member.
     */
    public List<StudyRoom> findRoomsByStudent(int studentId) throws SQLException {
        String sql = "SELECT r.*, s.student_name AS creator_name, " +
                     "(SELECT COUNT(*) FROM ROOM_MEMBERS rm WHERE rm.room_id = r.room_id AND rm.participant_status = 'ACTIVE') AS active_count, " +
                     "(SELECT COUNT(*) FROM STUDY_SESSIONS ss WHERE ss.room_id = r.room_id AND ss.session_status = 'ACTIVE') AS active_session_count " +
                     "FROM STUDY_ROOMS r " +
                     "JOIN STUDENTS s ON r.created_by = s.student_id " +
                     "JOIN ROOM_MEMBERS rm_curr ON r.room_id = rm_curr.room_id " +
                     "WHERE rm_curr.student_id = ? AND rm_curr.participant_status = 'ACTIVE' " +
                     "ORDER BY r.created_at DESC";

        List<StudyRoom> rooms = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rooms.add(mapResultSetToRoom(rs));
                }
            }
        }
        return rooms;
    }

    /**
     * Updates room status ('OPEN' or 'CLOSED').
     */
    public boolean updateRoomStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE STUDY_ROOMS SET room_status = ? WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());
            ps.setInt(2, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Gets the count of ACTIVE participants in a room.
     */
    public int getActiveParticipantCount(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ROOM_MEMBERS WHERE room_id = ? AND participant_status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Helper to read from ResultSet.
     */
    private StudyRoom mapResultSetToRoom(ResultSet rs) throws SQLException {
        StudyRoom r = new StudyRoom();
        r.setRoomId(rs.getInt("room_id"));
        r.setRoomName(rs.getString("room_name"));
        r.setSubject(rs.getString("subject"));
        r.setExamName(rs.getString("exam_name"));
        r.setCreatedBy(rs.getInt("created_by"));
        r.setRoomDescription(rs.getString("room_description"));
        r.setMaxParticipants(rs.getInt("max_participants"));
        r.setRoomStatus(rs.getString("room_status"));
        r.setCreatedAt(rs.getTimestamp("created_at"));

        try {
            r.setCreatorName(rs.getString("creator_name"));
        } catch (SQLException ignored) {}

        try {
            r.setActiveParticipantCount(rs.getInt("active_count"));
        } catch (SQLException ignored) {}

        try {
            r.setActiveSession(rs.getInt("active_session_count") > 0);
        } catch (SQLException ignored) {}

        return r;
    }
}
