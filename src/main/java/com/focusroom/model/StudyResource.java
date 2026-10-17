package com.focusroom.model;

import java.sql.Timestamp;

/**
 * Model representing learning resources shared within a Study Room.
 */
public class StudyResource {
    private int resourceId;
    private int roomId;
    private int studentId;
    private String resourceTitle;
    private String resourceLink;
    private Timestamp sharedAt;

    // Additional display metadata
    private String studentName;

    public StudyResource() {
    }

    public StudyResource(int roomId, int studentId, String resourceTitle, String resourceLink) {
        this.roomId = roomId;
        this.studentId = studentId;
        this.resourceTitle = resourceTitle;
        this.resourceLink = resourceLink;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
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

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public String getResourceLink() {
        return resourceLink;
    }

    public void setResourceLink(String resourceLink) {
        this.resourceLink = resourceLink;
    }

    public Timestamp getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Timestamp sharedAt) {
        this.sharedAt = sharedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Override
    public String toString() {
        return resourceTitle + " (" + resourceLink + ") shared by " + (studentName != null ? studentName : studentId);
    }
}
