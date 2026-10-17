package com.focusroom.service;

import com.focusroom.dao.RoomMemberDAO;
import com.focusroom.dao.StudyResourceDAO;
import com.focusroom.dao.StudyRoomDAO;
import com.focusroom.model.RoomMember;
import com.focusroom.model.StudyResource;
import com.focusroom.model.StudyRoom;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service managing study room lifecycles, memberships, and resource sharing.
 */
public class StudyRoomService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    private final StudyRoomDAO roomDAO;
    private final RoomMemberDAO memberDAO;
    private final StudyResourceDAO resourceDAO;

    public StudyRoomService() {
        this.roomDAO = new StudyRoomDAO();
        this.memberDAO = new RoomMemberDAO();
        this.resourceDAO = new StudyResourceDAO();
    }

    public StudyRoomService(StudyRoomDAO roomDAO, RoomMemberDAO memberDAO, StudyResourceDAO resourceDAO) {
        this.roomDAO = roomDAO;
        this.memberDAO = memberDAO;
        this.resourceDAO = resourceDAO;
    }

    /**
     * Creates a new study room and sets status to OPEN.
     */
    public StudyRoom createRoom(String name, String subject, String exam, String description,
                                int maxParticipants, int creatorId) throws IllegalArgumentException, SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Room name cannot be empty.");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be empty.");
        }
        if (exam == null || exam.trim().isEmpty()) {
            throw new IllegalArgumentException("Examination name cannot be empty.");
        }
        if (maxParticipants < 2) {
            throw new IllegalArgumentException("Maximum participants must be at least 2.");
        }
        if (maxParticipants > 100) {
            throw new IllegalArgumentException("Maximum participants cannot exceed 100.");
        }

        StudyRoom room = new StudyRoom(name.trim(), subject.trim(), exam.trim(), creatorId,
                description != null ? description.trim() : "", maxParticipants);

        boolean success = roomDAO.createRoom(room);
        if (!success) {
            throw new SQLException("Failed to create study room.");
        }

        return room;
    }

    /**
     * Finds open rooms filtered by optional subject and exam search criteria.
     */
    public List<StudyRoom> getAvailableRooms(String subjectFilter, String examFilter) throws SQLException {
        return roomDAO.findOpenRooms(subjectFilter, examFilter);
    }

    /**
     * Finds rooms where the student is currently an active member.
     */
    public List<StudyRoom> getRoomsForStudent(int studentId) throws SQLException {
        return roomDAO.findRoomsByStudent(studentId);
    }

    /**
     * Joins a room, enforcing room status, capacity limits, and transaction safety.
     */
    public boolean joinRoom(int roomId, int studentId) throws SQLException {
        return memberDAO.joinRoom(roomId, studentId);
    }

    /**
     * Leaves a study room and updates status to LEFT.
     */
    public boolean leaveRoom(int roomId, int studentId) throws SQLException {
        return memberDAO.leaveRoom(roomId, studentId);
    }

    /**
     * Retrieves room details including creator and active participant count.
     */
    public Optional<StudyRoom> getRoomDetails(int roomId) throws SQLException {
        return roomDAO.findById(roomId);
    }

    /**
     * Retrieves currently active members of the room.
     */
    public List<RoomMember> getActiveMembers(int roomId) throws SQLException {
        return memberDAO.getActiveMembers(roomId);
    }

    /**
     * Retrieves all past and present members of the room.
     */
    public List<RoomMember> getAllMembers(int roomId) throws SQLException {
        return memberDAO.getAllMembers(roomId);
    }

    /**
     * Checks if a student is an active member in a room.
     */
    public boolean isStudentActiveMember(int roomId, int studentId) throws SQLException {
        return memberDAO.isMemberActive(roomId, studentId);
    }

    /**
     * Shares a study resource within a room.
     * Enforces active room membership and validates URL format.
     */
    public StudyResource shareResource(int roomId, int studentId, String title, String link)
            throws IllegalArgumentException, IllegalStateException, SQLException {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource title is required.");
        }
        if (link == null || link.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource URL or link is required.");
        }

        String cleanLink = link.trim();
        if (!URL_PATTERN.matcher(cleanLink).matches() && !cleanLink.startsWith("http://") && !cleanLink.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL format. Please provide a valid link starting with http:// or https://");
        }

        // Verify that the student is an ACTIVE member
        if (!memberDAO.isMemberActive(roomId, studentId)) {
            throw new IllegalStateException("Only active members of this room can share resources.");
        }

        StudyResource resource = new StudyResource(roomId, studentId, title.trim(), cleanLink);
        boolean saved = resourceDAO.addResource(resource);
        if (!saved) {
            throw new SQLException("Failed to save resource record.");
        }

        return resource;
    }

    /**
     * Retrieves all resources shared in a room.
     */
    public List<StudyResource> getResources(int roomId) throws SQLException {
        return resourceDAO.getResourcesByRoom(roomId);
    }

    /**
     * Closes a room. Only the room creator can close the room.
     */
    public boolean closeRoom(int roomId, int studentId) throws SQLException {
        Optional<StudyRoom> roomOpt = roomDAO.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("Room not found.");
        }
        StudyRoom room = roomOpt.get();
        if (room.getCreatedBy() != studentId) {
            throw new IllegalStateException("Only the room creator can close this room.");
        }
        return roomDAO.updateRoomStatus(roomId, "CLOSED");
    }
}
