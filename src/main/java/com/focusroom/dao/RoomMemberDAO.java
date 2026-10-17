package com.focusroom.dao;

import com.focusroom.model.RoomMember;
import com.focusroom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for ROOM_MEMBERS table.
 * Implements transaction management to prevent overbooking and race conditions.
 */
public class RoomMemberDAO {

    /**
     * Attempts to join a room for a student.
     * Prevents overbooking using transactions and row locking.
     * Prevents duplicate active memberships and allows rejoining.
     *
     * @param roomId    the ID of the study room
     * @param studentId the ID of the student
     * @return true if join was successful
     * @throws SQLException if a database error occurs or capacity/status constraints are violated
     */
    public boolean joinRoom(int roomId, int studentId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Lock room row and check status & capacity
            String lockRoomSql = "SELECT room_status, max_participants FROM STUDY_ROOMS WHERE room_id = ? FOR UPDATE";
            String roomStatus = null;
            int maxCapacity = 0;

            try (PreparedStatement psLock = conn.prepareStatement(lockRoomSql)) {
                psLock.setInt(1, roomId);
                try (ResultSet rs = psLock.executeQuery()) {
                    if (rs.next()) {
                        roomStatus = rs.getString("room_status");
                        maxCapacity = rs.getInt("max_participants");
                    } else {
                        conn.rollback();
                        throw new SQLException("Study room not found.");
                    }
                }
            }

            if (!"OPEN".equalsIgnoreCase(roomStatus)) {
                conn.rollback();
                throw new SQLException("Cannot join room: The room is currently CLOSED.");
            }

            // 2. Check existing active membership for this student
            String checkMemberSql = "SELECT participant_status FROM ROOM_MEMBERS WHERE room_id = ? AND student_id = ?";
            String currentStatus = null;
            boolean recordExists = false;

            try (PreparedStatement psCheck = conn.prepareStatement(checkMemberSql)) {
                psCheck.setInt(1, roomId);
                psCheck.setInt(2, studentId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        recordExists = true;
                        currentStatus = rs.getString("participant_status");
                    }
                }
            }

            if (recordExists && "ACTIVE".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                throw new SQLException("You are already an active participant in this study room.");
            }

            // 3. Count current active participants
            String countActiveSql = "SELECT COUNT(*) FROM ROOM_MEMBERS WHERE room_id = ? AND participant_status = 'ACTIVE'";
            int currentActiveCount = 0;
            try (PreparedStatement psCount = conn.prepareStatement(countActiveSql)) {
                psCount.setInt(1, roomId);
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) {
                        currentActiveCount = rs.getInt(1);
                    }
                }
            }

            if (currentActiveCount >= maxCapacity) {
                conn.rollback();
                throw new SQLException("Cannot join room: Maximum participant capacity (" + maxCapacity + ") has been reached.");
            }

            // 4. Insert new record or update existing LEFT record to ACTIVE
            if (recordExists) {
                String updateSql = "UPDATE ROOM_MEMBERS SET participant_status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP " +
                                  "WHERE room_id = ? AND student_id = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, roomId);
                    psUpdate.setInt(2, studentId);
                    psUpdate.executeUpdate();
                }
            } else {
                String insertSql = "INSERT INTO ROOM_MEMBERS (room_id, student_id, joined_at, participant_status) " +
                                  "VALUES (?, ?, CURRENT_TIMESTAMP, 'ACTIVE')";
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, roomId);
                    psInsert.setInt(2, studentId);
                    psInsert.executeUpdate();
                }
            }

            conn.commit();
            return true;
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
     * Sets a student's participant status to LEFT.
     */
    public boolean leaveRoom(int roomId, int studentId) throws SQLException {
        String sql = "UPDATE ROOM_MEMBERS SET participant_status = 'LEFT' " +
                     "WHERE room_id = ? AND student_id = ? AND participant_status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a student is an ACTIVE participant in a room.
     */
    public boolean isMemberActive(int roomId, int studentId) throws SQLException {
        String sql = "SELECT 1 FROM ROOM_MEMBERS WHERE room_id = ? AND student_id = ? AND participant_status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves all ACTIVE members of a room with their student details.
     */
    public List<RoomMember> getActiveMembers(int roomId) throws SQLException {
        String sql = "SELECT rm.*, s.student_name, s.email, s.department, s.year_of_study " +
                     "FROM ROOM_MEMBERS rm " +
                     "JOIN STUDENTS s ON rm.student_id = s.student_id " +
                     "WHERE rm.room_id = ? AND rm.participant_status = 'ACTIVE' " +
                     "ORDER BY rm.joined_at ASC";

        List<RoomMember> members = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToRoomMember(rs));
                }
            }
        }
        return members;
    }

    /**
     * Retrieves ALL members (ACTIVE and LEFT) of a room.
     */
    public List<RoomMember> getAllMembers(int roomId) throws SQLException {
        String sql = "SELECT rm.*, s.student_name, s.email, s.department, s.year_of_study " +
                     "FROM ROOM_MEMBERS rm " +
                     "JOIN STUDENTS s ON rm.student_id = s.student_id " +
                     "WHERE rm.room_id = ? " +
                     "ORDER BY rm.joined_at DESC";

        List<RoomMember> members = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToRoomMember(rs));
                }
            }
        }
        return members;
    }

    private RoomMember mapResultSetToRoomMember(ResultSet rs) throws SQLException {
        RoomMember rm = new RoomMember();
        rm.setMembershipId(rs.getInt("membership_id"));
        rm.setRoomId(rs.getInt("room_id"));
        rm.setStudentId(rs.getInt("student_id"));
        rm.setJoinedAt(rs.getTimestamp("joined_at"));
        rm.setParticipantStatus(rs.getString("participant_status"));
        rm.setStudentName(rs.getString("student_name"));
        rm.setStudentEmail(rs.getString("email"));
        rm.setDepartment(rs.getString("department"));
        rm.setYearOfStudy(rs.getInt("year_of_study"));
        return rm;
    }
}
