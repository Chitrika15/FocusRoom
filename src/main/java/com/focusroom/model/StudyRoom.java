package com.focusroom.model;

import java.sql.Timestamp;

/**
 * Model representing a Virtual Study Room.
 */
public class StudyRoom {
    private int roomId;
    private String roomName;
    private String subject;
    private String examName;
    private int createdBy;
    private String roomDescription;
    private int maxParticipants;
    private String roomStatus; // OPEN or CLOSED
    private Timestamp createdAt;

    // Additional display metadata
    private String creatorName;
    private int activeParticipantCount;
    private boolean activeSession;

    public StudyRoom() {
        this.roomStatus = "OPEN";
    }

    public StudyRoom(String roomName, String subject, String examName, int createdBy, String roomDescription, int maxParticipants) {
        this.roomName = roomName;
        this.subject = subject;
        this.examName = examName;
        this.createdBy = createdBy;
        this.roomDescription = roomDescription;
        this.maxParticipants = maxParticipants;
        this.roomStatus = "OPEN";
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
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

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getRoomDescription() {
        return roomDescription;
    }

    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public int getActiveParticipantCount() {
        return activeParticipantCount;
    }

    public void setActiveParticipantCount(int activeParticipantCount) {
        this.activeParticipantCount = activeParticipantCount;
    }

    public boolean hasActiveSession() {
        return activeSession;
    }

    public void setActiveSession(boolean activeSession) {
        this.activeSession = activeSession;
    }

    public boolean isFull() {
        return activeParticipantCount >= maxParticipants;
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(roomStatus);
    }

    @Override
    public String toString() {
        return roomName + " [" + subject + " - " + examName + "] (" + activeParticipantCount + "/" + maxParticipants + ")";
    }
}
