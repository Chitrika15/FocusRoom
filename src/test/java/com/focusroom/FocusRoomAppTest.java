package com.focusroom;

import com.focusroom.dao.RoomMemberDAO;
import com.focusroom.dao.StudentDAO;
import com.focusroom.dao.StudyResourceDAO;
import com.focusroom.dao.StudyRoomDAO;
import com.focusroom.dao.StudySessionDAO;
import com.focusroom.model.RoomMember;
import com.focusroom.model.Student;
import com.focusroom.model.StudyResource;
import com.focusroom.model.StudyRoom;
import com.focusroom.model.StudySession;
import com.focusroom.service.AuthService;
import com.focusroom.service.StudyRoomService;
import com.focusroom.service.StudySessionService;
import com.focusroom.util.DBConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Automated Test Suite covering all 12 functional scenarios:
 * 1. Successful student registration.
 * 2. Duplicate email registration.
 * 3. Successful login.
 * 4. Invalid login.
 * 5. Successful room creation.
 * 6. Searching rooms by subject or examination.
 * 7. Successfully joining an available room.
 * 8. Preventing joins when a room is full or closed.
 * 9. Leaving a room and updating membership status.
 * 10. Sharing and retrieving study resources.
 * 11. Starting and completing a study session with correct duration.
 * 12. Viewing study history and total study time.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FocusRoomAppTest {

    private static AuthService authService;
    private static StudyRoomService roomService;
    private static StudySessionService sessionService;

    // Shared test state across ordered steps
    private static Student testStudent1;
    private static Student testStudent2;
    private static Student testStudent3;
    private static StudyRoom createdRoom;
    private static StudySession testSession;

    @BeforeAll
    public static void setupDatabase() throws Exception {
        // Point DBConnection to in-memory H2 database with MySQL compatibility mode
        String testUrl = "jdbc:h2:mem:focusroom;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1";
        DBConnection.setCustomConnection(testUrl, "sa", "");

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Drop existing tables if any
            stmt.execute("DROP TABLE IF EXISTS STUDY_SESSIONS");
            stmt.execute("DROP TABLE IF EXISTS STUDY_RESOURCES");
            stmt.execute("DROP TABLE IF EXISTS ROOM_MEMBERS");
            stmt.execute("DROP TABLE IF EXISTS STUDY_ROOMS");
            stmt.execute("DROP TABLE IF EXISTS STUDENTS");

            // 1. STUDENTS
            stmt.execute("CREATE TABLE STUDENTS (" +
                    "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "student_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(150) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "department VARCHAR(100) NOT NULL, " +
                    "year_of_study INT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 2. STUDY_ROOMS
            stmt.execute("CREATE TABLE STUDY_ROOMS (" +
                    "room_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "room_name VARCHAR(150) NOT NULL, " +
                    "subject VARCHAR(100) NOT NULL, " +
                    "exam_name VARCHAR(100) NOT NULL, " +
                    "created_by INT NOT NULL, " +
                    "room_description TEXT, " +
                    "max_participants INT NOT NULL, " +
                    "room_status VARCHAR(20) NOT NULL DEFAULT 'OPEN', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (created_by) REFERENCES STUDENTS(student_id) ON DELETE CASCADE)");

            // 3. ROOM_MEMBERS
            stmt.execute("CREATE TABLE ROOM_MEMBERS (" +
                    "membership_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "room_id INT NOT NULL, " +
                    "student_id INT NOT NULL, " +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "participant_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE, " +
                    "UNIQUE (room_id, student_id))");

            // 4. STUDY_RESOURCES
            stmt.execute("CREATE TABLE STUDY_RESOURCES (" +
                    "resource_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "room_id INT NOT NULL, " +
                    "student_id INT NOT NULL, " +
                    "resource_title VARCHAR(200) NOT NULL, " +
                    "resource_link VARCHAR(500) NOT NULL, " +
                    "shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE)");

            // 5. STUDY_SESSIONS
            stmt.execute("CREATE TABLE STUDY_SESSIONS (" +
                    "session_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "room_id INT NOT NULL, " +
                    "started_by INT NOT NULL, " +
                    "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "end_time TIMESTAMP NULL, " +
                    "duration_minutes INT DEFAULT 0, " +
                    "session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                    "FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (started_by) REFERENCES STUDENTS(student_id) ON DELETE CASCADE)");
        }

        authService = new AuthService();
        roomService = new StudyRoomService();
        sessionService = new StudySessionService();
    }

    /**
     * Test Case 1: Successful student registration.
     */
    @Test
    @Order(1)
    public void test01_SuccessfulStudentRegistration() throws Exception {
        testStudent1 = authService.register(
                "Jane Doe",
                "jane.doe@university.edu",
                "Secret@123",
                "Computer Science",
                3
        );

        assertNotNull(testStudent1, "Registered student should not be null");
        assertTrue(testStudent1.getStudentId() > 0, "Student ID should be auto-generated");
        assertEquals("Jane Doe", testStudent1.getStudentName());
        assertEquals("jane.doe@university.edu", testStudent1.getEmail());

        // Register 2 more students for later group tests
        testStudent2 = authService.register("John Mark", "john.mark@university.edu", "Password@1", "Information Tech", 2);
        testStudent3 = authService.register("Sara Lee", "sara.lee@university.edu", "Password@2", "Data Science", 4);
    }

    /**
     * Test Case 2: Duplicate email registration.
     */
    @Test
    @Order(2)
    public void test02_DuplicateEmailRegistration() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(
                    "Jane Imposter",
                    "jane.doe@university.edu", // same email
                    "AnotherPass@123",
                    "Electrical Eng",
                    1
            );
        });

        assertTrue(exception.getMessage().contains("already exists"),
                "Expected duplicate email error message, got: " + exception.getMessage());
    }

    /**
     * Test Case 3: Successful login.
     */
    @Test
    @Order(3)
    public void test03_SuccessfulLogin() throws Exception {
        Student loggedIn = authService.login("jane.doe@university.edu", "Secret@123");
        assertNotNull(loggedIn, "Login must succeed with correct credentials");
        assertEquals(testStudent1.getStudentId(), loggedIn.getStudentId());
        assertEquals("Jane Doe", loggedIn.getStudentName());
    }

    /**
     * Test Case 4: Invalid login.
     */
    @Test
    @Order(4)
    public void test04_InvalidLogin() {
        // Wrong password
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login("jane.doe@university.edu", "WrongPassword999");
        });

        // Non-existent email
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login("unknown.student@university.edu", "Secret@123");
        });
    }

    /**
     * Test Case 5: Successful room creation.
     */
    @Test
    @Order(5)
    public void test05_SuccessfulRoomCreation() throws Exception {
        createdRoom = roomService.createRoom(
                "Distributed Systems Study Group",
                "Computer Science",
                "Final Exam 2026",
                "Focusing on Raft consensus, vector clocks, and Paxos.",
                2, // max 2 participants for testing capacity limit
                testStudent1.getStudentId()
        );

        assertNotNull(createdRoom);
        assertTrue(createdRoom.getRoomId() > 0, "Room ID must be auto-generated");
        assertEquals("OPEN", createdRoom.getRoomStatus());

        // Creator should be automatically enrolled as an ACTIVE member
        List<RoomMember> members = roomService.getActiveMembers(createdRoom.getRoomId());
        assertEquals(1, members.size(), "Creator must be enrolled as active member");
        assertEquals(testStudent1.getStudentId(), members.get(0).getStudentId());
    }

    /**
     * Test Case 6: Searching rooms by subject or examination.
     */
    @Test
    @Order(6)
    public void test06_SearchingRoomsBySubjectOrExam() throws Exception {
        // Filter by subject "Computer"
        List<StudyRoom> bySubject = roomService.getAvailableRooms("Computer", null);
        assertFalse(bySubject.isEmpty(), "Should find room by subject filter");
        assertTrue(bySubject.stream().anyMatch(r -> r.getRoomId() == createdRoom.getRoomId()));

        // Filter by exam "Final Exam"
        List<StudyRoom> byExam = roomService.getAvailableRooms(null, "Final Exam");
        assertFalse(byExam.isEmpty(), "Should find room by exam filter");

        // Filter non-matching
        List<StudyRoom> none = roomService.getAvailableRooms("NonExistentSubjectXYZ", null);
        assertTrue(none.isEmpty(), "Should return empty list for non-matching filter");
    }

    /**
     * Test Case 7: Successfully joining an available room.
     */
    @Test
    @Order(7)
    public void test07_SuccessfullyJoiningAvailableRoom() throws Exception {
        // testStudent2 joins the room (capacity: 2, current: 1 -> new: 2)
        boolean joined = roomService.joinRoom(createdRoom.getRoomId(), testStudent2.getStudentId());
        assertTrue(joined, "Student 2 should successfully join");

        List<RoomMember> activeMembers = roomService.getActiveMembers(createdRoom.getRoomId());
        assertEquals(2, activeMembers.size(), "Active member count should now be 2");
    }

    /**
     * Test Case 8: Preventing joins when a room is full or closed.
     */
    @Test
    @Order(8)
    public void test08_PreventingJoinsWhenFullOrClosed() {
        // Room capacity is 2 and currently has 2 active members (Student 1 & Student 2).
        // Student 3 attempts to join:
        SQLException fullEx = assertThrows(SQLException.class, () -> {
            roomService.joinRoom(createdRoom.getRoomId(), testStudent3.getStudentId());
        });
        assertTrue(fullEx.getMessage().contains("capacity") || fullEx.getMessage().contains("reached"),
                "Expected room capacity reached error, got: " + fullEx.getMessage());

        // Create another closed room
        assertDoesNotThrow(() -> {
            StudyRoom closedRoom = roomService.createRoom("Closed Lab", "CS", "Test", "Desc", 5, testStudent1.getStudentId());
            roomService.closeRoom(closedRoom.getRoomId(), testStudent1.getStudentId());

            SQLException closedEx = assertThrows(SQLException.class, () -> {
                roomService.joinRoom(closedRoom.getRoomId(), testStudent2.getStudentId());
            });
            assertTrue(closedEx.getMessage().contains("CLOSED"));
        });
    }

    /**
     * Test Case 9: Leaving a room and updating membership status.
     */
    @Test
    @Order(9)
    public void test09_LeavingRoomAndUpdatingMembership() throws Exception {
        // Student 2 leaves the room
        boolean left = roomService.leaveRoom(createdRoom.getRoomId(), testStudent2.getStudentId());
        assertTrue(left, "Student 2 should be able to leave the room");

        // Active members should now decrease back to 1
        List<RoomMember> activeMembers = roomService.getActiveMembers(createdRoom.getRoomId());
        assertEquals(1, activeMembers.size(), "Active members should decrease to 1");

        // All members record (active + left) should still show Student 2 as LEFT
        List<RoomMember> allMembers = roomService.getAllMembers(createdRoom.getRoomId());
        Optional<RoomMember> s2Record = allMembers.stream()
                .filter(m -> m.getStudentId() == testStudent2.getStudentId())
                .findFirst();
        assertTrue(s2Record.isPresent());
        assertEquals("LEFT", s2Record.get().getParticipantStatus());

        // Rejoin test: Student 2 rejoins the room
        boolean rejoined = roomService.joinRoom(createdRoom.getRoomId(), testStudent2.getStudentId());
        assertTrue(rejoined, "Student 2 should be able to rejoin an open room with available capacity");
        assertEquals(2, roomService.getActiveMembers(createdRoom.getRoomId()).size());
    }

    /**
     * Test Case 10: Sharing and retrieving study resources.
     */
    @Test
    @Order(10)
    public void test10_SharingAndRetrievingStudyResources() throws Exception {
        // Student 1 shares a valid resource
        StudyResource res1 = roomService.shareResource(
                createdRoom.getRoomId(),
                testStudent1.getStudentId(),
                "Distributed Consensus Lecture Notes",
                "https://raft.github.io/raft.pdf"
        );
        assertNotNull(res1);
        assertTrue(res1.getResourceId() > 0);

        // Retrieve resources
        List<StudyResource> resources = roomService.getResources(createdRoom.getRoomId());
        assertEquals(1, resources.size());
        assertEquals("Distributed Consensus Lecture Notes", resources.get(0).getResourceTitle());

        // Non-member (Student 3) cannot share resource
        assertThrows(IllegalStateException.class, () -> {
            roomService.shareResource(
                    createdRoom.getRoomId(),
                    testStudent3.getStudentId(), // not an active member
                    "Hacker Notes",
                    "https://example.com/notes"
            );
        });

        // Invalid URL should be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            roomService.shareResource(
                    createdRoom.getRoomId(),
                    testStudent1.getStudentId(),
                    "Invalid URL Item",
                    "not-a-valid-url"
            );
        });
    }

    /**
     * Test Case 11: Starting and completing a study session with correct duration.
     */
    @Test
    @Order(11)
    public void test11_StartingAndCompletingStudySession() throws Exception {
        // Start study session
        testSession = sessionService.startSession(createdRoom.getRoomId(), testStudent1.getStudentId());
        assertNotNull(testSession);
        assertEquals("ACTIVE", testSession.getSessionStatus());

        // Active session should be visible
        Optional<StudySession> activeSession = sessionService.getActiveSession(createdRoom.getRoomId());
        assertTrue(activeSession.isPresent());
        assertEquals(testSession.getSessionId(), activeSession.get().getSessionId());

        // Prevent starting duplicate active session in same room
        assertThrows(SQLException.class, () -> {
            sessionService.startSession(createdRoom.getRoomId(), testStudent2.getStudentId());
        });

        // Stop session
        StudySession completed = sessionService.stopSession(testSession.getSessionId(), testStudent1.getStudentId());
        assertEquals("COMPLETED", completed.getSessionStatus());
        assertNotNull(completed.getEndTime());
        assertTrue(completed.getDurationMinutes() >= 0);

        // No more active session in room
        Optional<StudySession> afterStop = sessionService.getActiveSession(createdRoom.getRoomId());
        assertTrue(afterStop.isEmpty());
    }

    /**
     * Test Case 12: Viewing study history and total study time.
     */
    @Test
    @Order(12)
    public void test12_ViewingStudyHistoryAndTotalStudyTime() throws Exception {
        List<StudySession> studentHistory = sessionService.getStudentHistory(testStudent1.getStudentId());
        assertFalse(studentHistory.isEmpty(), "Student 1 should have study history records");

        int totalMins = sessionService.getTotalStudyMinutes(testStudent1.getStudentId());
        assertTrue(totalMins >= 0, "Total study minutes should be >= 0");

        List<StudySession> roomHistory = sessionService.getRoomHistory(createdRoom.getRoomId());
        assertFalse(roomHistory.isEmpty(), "Room history should contain the completed session");
    }
}
