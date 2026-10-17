package com.focusroom.dao;

import com.focusroom.model.Student;
import com.focusroom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Data Access Object for STUDENTS table.
 */
public class StudentDAO {

    /**
     * Inserts a new student into the database.
     *
     * @param student Student model containing details and hashed password
     * @return true if successfully inserted, false otherwise
     * @throws SQLException if a database error occurs (e.g. duplicate email)
     */
    public boolean createStudent(Student student) throws SQLException {
        String sql = "INSERT INTO STUDENTS (student_name, email, password, department, year_of_study) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getStudentName());
            ps.setString(2, student.getEmail().toLowerCase().trim());
            ps.setString(3, student.getPassword());
            ps.setString(4, student.getDepartment());
            ps.setInt(5, student.getYearOfStudy());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        student.setStudentId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Finds a student by email address.
     *
     * @param email student email
     * @return Optional of Student
     * @throws SQLException if query fails
     */
    public Optional<Student> findByEmail(String email) throws SQLException {
        String sql = "SELECT student_id, student_name, email, password, department, year_of_study, created_at " +
                     "FROM STUDENTS WHERE LOWER(email) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.toLowerCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    return Optional.of(student);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a student by primary key ID.
     *
     * @param studentId the student id
     * @return Optional of Student
     * @throws SQLException if query fails
     */
    public Optional<Student> findById(int studentId) throws SQLException {
        String sql = "SELECT student_id, student_name, email, password, department, year_of_study, created_at " +
                     "FROM STUDENTS WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student student = mapResultSetToStudent(rs);
                    return Optional.of(student);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if an email is already registered.
     */
    public boolean isEmailRegistered(String email) throws SQLException {
        String sql = "SELECT 1 FROM STUDENTS WHERE LOWER(email) = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.toLowerCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setStudentName(rs.getString("student_name"));
        s.setEmail(rs.getString("email"));
        s.setPassword(rs.getString("password"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }
}
