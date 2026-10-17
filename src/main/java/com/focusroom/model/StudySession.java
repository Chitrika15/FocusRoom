package com.focusroom.model;

import java.sql.Timestamp;

/**
 * Model representing a study session conducted in a room.
 */
public class StudySession {
    private int sessionId;
    private int roomId;
    private int startedBy;
    private Timestamp startTime;
    private Timestamp endTime;
    private int durationMinutes;
    private String sessionStatus; // SCHEDULED, ACTIVE, or COMPLETED

    // Display metadata
    private String roomName;
    private String subject;
    private String startedByName;

    public StudySession() {
        this.sessionStatus = "ACTIVE";
    }

    public StudySession(int roomId, int startedBy) {
        this.roomId = roomId;
        this.startedBy = startedBy;
        this.sessionStatus = "ACTIVE";
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(int startedBy) {
        this.startedBy = startedBy;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStartedByName() {
        return startedByName;
    }

    public void setStartedByName(String startedByName) {
        this.startedByName = startedByName;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(sessionStatus);
    }

    @Override
    public String toString() {
        return "Session #" + sessionId + " in " + roomName + " [" + sessionStatus + "] Duration: " + durationMinutes + " mins";
    }
}
