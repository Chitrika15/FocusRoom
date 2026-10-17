package com.focusroom.service;

import com.focusroom.dao.StudentDAO;
import com.focusroom.model.Student;
import com.focusroom.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service managing student authentication and registration workflows.
 */
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final StudentDAO studentDAO;

    public AuthService() {
        this.studentDAO = new StudentDAO();
    }

    public AuthService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Registers a new student after thorough validation.
     *
     * @param name        Full name of the student
     * @param email       Valid unique university/personal email
     * @param rawPassword Plain text password
     * @param department  Department / Major
     * @param yearOfStudy Year of study (1 to 5)
     * @return registered Student object with generated studentId
     * @throws IllegalArgumentException on validation failures
     * @throws SQLException             on database errors
     */
    public Student register(String name, String email, String rawPassword, String department, int yearOfStudy)
            throws IllegalArgumentException, SQLException {

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name is required.");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("Student name must be at least 2 characters long.");
        }

        // Validate email
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Invalid email format (e.g., student@university.edu).");
        }

        // Check duplicate email
        if (studentDAO.isEmailRegistered(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        // Validate password
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        // Validate department
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required.");
        }

        // Validate year of study
        if (yearOfStudy < 1 || yearOfStudy > 5) {
            throw new IllegalArgumentException("Year of study must be between 1 and 5.");
        }

        // Hash password securely with BCrypt
        String hashedPassword = PasswordUtil.hashPassword(rawPassword);

        Student student = new Student(name.trim(), cleanEmail, hashedPassword, department.trim(), yearOfStudy);
        boolean created = studentDAO.createStudent(student);

        if (!created) {
            throw new SQLException("Failed to save student record to database.");
        }

        return student;
    }

    /**
     * Authenticates a student against stored credentials.
     *
     * @param email       student email
     * @param rawPassword plain text password
     * @return Student object upon successful login
     * @throws IllegalArgumentException on validation failure or incorrect credentials
     * @throws SQLException             on database error
     */
    public Student login(String email, String rawPassword) throws IllegalArgumentException, SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter your email.");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter your password.");
        }

        Optional<Student> studentOpt = studentDAO.findByEmail(email.trim().toLowerCase());
        if (studentOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        Student student = studentOpt.get();
        boolean matches = PasswordUtil.verifyPassword(rawPassword, student.getPassword());
        if (!matches) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return student;
    }
}
