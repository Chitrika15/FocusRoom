package com.focusroom.model;

import java.sql.Timestamp;

/**
 * Model representing a registered Student.
 */
public class Student {
    private int studentId;
    private String studentName;
    private String email;
    private String password; // Hashed password
    private String department;
    private int yearOfStudy;
    private Timestamp createdAt;

    public Student() {
    }

    public Student(String studentName, String email, String password, String department, int yearOfStudy) {
        this.studentName = studentName;
        this.email = email;
        this.password = password;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
    }

    public Student(int studentId, String studentName, String email, String password, String department, int yearOfStudy) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.password = password;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(int yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return studentName + " (" + email + " - Year " + yearOfStudy + " " + department + ")";
    }
}
