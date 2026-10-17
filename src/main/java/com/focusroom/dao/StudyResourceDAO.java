package com.focusroom.dao;

import com.focusroom.model.StudyResource;
import com.focusroom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for STUDY_RESOURCES table.
 */
public class StudyResourceDAO {

    /**
     * Inserts a new study resource into the database.
     *
     * @param resource StudyResource object
     * @return true if inserted successfully
     * @throws SQLException if a database error occurs
     */
    public boolean addResource(StudyResource resource) throws SQLException {
        String sql = "INSERT INTO STUDY_RESOURCES (room_id, student_id, resource_title, resource_link, shared_at) " +
                     "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, resource.getRoomId());
            ps.setInt(2, resource.getStudentId());
            ps.setString(3, resource.getResourceTitle());
            ps.setString(4, resource.getResourceLink());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        resource.setResourceId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Retrieves all study resources shared in a specific room.
     *
     * @param roomId room identifier
     * @return list of resources shared in the room
     * @throws SQLException if query fails
     */
    public List<StudyResource> getResourcesByRoom(int roomId) throws SQLException {
        String sql = "SELECT sr.*, s.student_name " +
                     "FROM STUDY_RESOURCES sr " +
                     "JOIN STUDENTS s ON sr.student_id = s.student_id " +
                     "WHERE sr.room_id = ? " +
                     "ORDER BY sr.shared_at DESC";

        List<StudyResource> resources = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudyResource r = new StudyResource();
                    r.setResourceId(rs.getInt("resource_id"));
                    r.setRoomId(rs.getInt("room_id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setResourceTitle(rs.getString("resource_title"));
                    r.setResourceLink(rs.getString("resource_link"));
                    r.setSharedAt(rs.getTimestamp("shared_at"));
                    r.setStudentName(rs.getString("student_name"));
                    resources.add(r);
                }
            }
        }
        return resources;
    }
}
