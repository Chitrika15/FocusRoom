-- =======================================================
-- FocusRoom - Virtual Study Rooms Database Initialization
-- =======================================================

CREATE DATABASE IF NOT EXISTS focusroom
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE focusroom;

-- Drop tables in reverse order of foreign key dependencies if re-running
DROP TABLE IF EXISTS STUDY_SESSIONS;
DROP TABLE IF EXISTS STUDY_RESOURCES;
DROP TABLE IF EXISTS ROOM_MEMBERS;
DROP TABLE IF EXISTS STUDY_ROOMS;
DROP TABLE IF EXISTS STUDENTS;

-- 1. STUDENTS Table
CREATE TABLE STUDENTS (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    year_of_study INT NOT NULL CHECK (year_of_study BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. STUDY_ROOMS Table
CREATE TABLE STUDY_ROOMS (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(150) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    exam_name VARCHAR(100) NOT NULL,
    created_by INT NOT NULL,
    room_description TEXT,
    max_participants INT NOT NULL CHECK (max_participants >= 2),
    room_status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (room_status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,
    INDEX idx_room_subject (subject),
    INDEX idx_room_exam (exam_name),
    INDEX idx_room_status (room_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. ROOM_MEMBERS Table
CREATE TABLE ROOM_MEMBERS (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    student_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    participant_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (participant_status IN ('ACTIVE', 'LEFT')),
    FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,
    UNIQUE KEY uq_room_student (room_id, student_id),
    INDEX idx_member_room_status (room_id, participant_status),
    INDEX idx_member_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. STUDY_RESOURCES Table
CREATE TABLE STUDY_RESOURCES (
    resource_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    student_id INT NOT NULL,
    resource_title VARCHAR(200) NOT NULL,
    resource_link VARCHAR(500) NOT NULL,
    shared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,
    INDEX idx_resource_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. STUDY_SESSIONS Table
CREATE TABLE STUDY_SESSIONS (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    started_by INT NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    duration_minutes INT DEFAULT 0,
    session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (session_status IN ('SCHEDULED', 'ACTIVE', 'COMPLETED')),
    FOREIGN KEY (room_id) REFERENCES STUDY_ROOMS(room_id) ON DELETE CASCADE,
    FOREIGN KEY (started_by) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,
    INDEX idx_session_room_status (room_id, session_status),
    INDEX idx_session_started_by (started_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample Seed Data (Passwords hashed for 'Password@123' using BCrypt)
INSERT INTO STUDENTS (student_name, email, password, department, year_of_study)
VALUES
('Alice Johnson', 'alice@university.edu', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Computer Science', 3),
('Bob Smith', 'bob@university.edu', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Information Technology', 2),
('Charlie Davis', 'charlie@university.edu', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Electronics & Comm', 4);

INSERT INTO STUDY_ROOMS (room_name, subject, exam_name, created_by, room_description, max_participants, room_status)
VALUES
('Algorithms & Data Structures Sprint', 'Computer Science', 'Semester End Exam', 1, 'Group solving dynamic programming and graph problems.', 6, 'OPEN'),
('Database Systems Deep Dive', 'Information Technology', 'Midterm Assessment', 2, 'Relational calculus, normal forms, and SQL transaction drills.', 8, 'OPEN');

INSERT INTO ROOM_MEMBERS (room_id, student_id, joined_at, participant_status)
VALUES
(1, 1, CURRENT_TIMESTAMP, 'ACTIVE'),
(1, 2, CURRENT_TIMESTAMP, 'ACTIVE'),
(2, 2, CURRENT_TIMESTAMP, 'ACTIVE'),
(2, 3, CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO STUDY_RESOURCES (room_id, student_id, resource_title, resource_link, shared_at)
VALUES
(1, 1, 'Graph Algorithms Cheat Sheet', 'https://cp-algorithms.com/graph/breadth-first-search.html', CURRENT_TIMESTAMP),
(1, 2, 'LeetCode Curated 75 Problems', 'https://leetcode.com/discuss/general-discussion/460599/blind-75-leetcode-questions', CURRENT_TIMESTAMP),
(2, 2, 'MySQL Official Transaction Isolation Docs', 'https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html', CURRENT_TIMESTAMP);
