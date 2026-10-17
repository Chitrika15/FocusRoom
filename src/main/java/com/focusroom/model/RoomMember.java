package com.focusroom.model;

import java.sql.Timestamp;

/**
 * Model representing a student's membership in a Study Room.
 */
public class RoomMember {
    private int membershipId;
    private int roomId;
    private int studentId;
    private Timestamp joinedAt;
    private String participantStatus; // ACTIVE or LEFT

    // Student details for UI display
    private String studentName;
    private String studentEmail;
    private String department;
    private int yearOfStudy;

    public RoomMember() {
        this.participantStatus = "ACTIVE";
    }

    public RoomMember(int roomId, int studentId) {
        this.roomId = roomId;
        this.studentId = studentId;
        this.participantStatus = "ACTIVE";
    }

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        this.membershipId = membershipId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getParticipantStatus() {
        return participantStatus;
    }

    public void setParticipantStatus(String participantStatus) {
        this.participantStatus = participantStatus;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
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

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(participantStatus);
    }

    @Override
    public String toString() {
        return studentName + " (" + participantStatus + ")";
    }
}
