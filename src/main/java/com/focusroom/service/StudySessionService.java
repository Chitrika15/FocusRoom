package com.focusroom.service;

import com.focusroom.dao.RoomMemberDAO;
import com.focusroom.dao.StudySessionDAO;
import com.focusroom.model.StudySession;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing study session lifecycles, timer synchronization, and history calculation.
 */
public class StudySessionService {

    private final StudySessionDAO sessionDAO;
    private final RoomMemberDAO memberDAO;

    public StudySessionService() {
        this.sessionDAO = new StudySessionDAO();
        this.memberDAO = new RoomMemberDAO();
    }

    public StudySessionService(StudySessionDAO sessionDAO, RoomMemberDAO memberDAO) {
        this.sessionDAO = sessionDAO;
        this.memberDAO = memberDAO;
    }

    /**
     * Starts a new study session in a room.
     * Enforces active room membership and prevents multiple concurrent active sessions.
     */
    public StudySession startSession(int roomId, int studentId) throws IllegalStateException, SQLException {
        if (!memberDAO.isMemberActive(roomId, studentId)) {
            throw new IllegalStateException("Only active members of this room can start a study session.");
        }

        // DAO performs atomic transactional check for existing active session
        return sessionDAO.startSession(roomId, studentId);
    }

    /**
     * Stops an active study session.
     * Computes the exact duration based on timestamps and sets status to COMPLETED.
     */
    public StudySession stopSession(int sessionId, int studentId) throws IllegalStateException, SQLException {
        // Any active member can stop or student who started
        return sessionDAO.stopSession(sessionId);
    }

    /**
     * Retrieves the active session in a room, if running.
     */
    public Optional<StudySession> getActiveSession(int roomId) throws SQLException {
        return sessionDAO.getActiveSession(roomId);
    }

    /**
     * Retrieves study history for a specific student.
     */
    public List<StudySession> getStudentHistory(int studentId) throws SQLException {
        return sessionDAO.getSessionsByStudent(studentId);
    }

    /**
     * Retrieves all study sessions conducted within a room.
     */
    public List<StudySession> getRoomHistory(int roomId) throws SQLException {
        return sessionDAO.getSessionsByRoom(roomId);
    }

    /**
     * Calculates the total completed study minutes for a student.
     */
    public int getTotalStudyMinutes(int studentId) throws SQLException {
        return sessionDAO.getTotalStudyMinutesForStudent(studentId);
    }
}
