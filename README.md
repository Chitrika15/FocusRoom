# FocusRoom – Virtual Study Rooms

A complete Java Swing desktop application for student collaboration, virtual study rooms, resource sharing, focus timers, and study history tracking powered by **Java, Maven, JDBC, and MySQL**.

---

## 1. Project Overview & Features

FocusRoom offers 8 core modules:
1. **Student Registration & Login**: Validated registration, email format check, duplicate prevention, and secure password hashing using **BCrypt**.
2. **Create Study Rooms**: Create customized rooms with subject, exam target, participant capacity limit (2-50), description, and default `OPEN` status. Automatically enrolls the creator.
3. **Discover & Join Rooms**: Search and filter rooms in real time by subject and examination name with live participant counts and active session indicator.
4. **Manage Room Membership**: Atomic transaction joins preventing overbooking and duplicate active memberships. Real-time member list and status toggling (`ACTIVE` / `LEFT`).
5. **Share Study Resources**: Share links and study notes inside rooms with validation and one-click browser opening.
6. **Shared Study Timer**: Synchronized study timer with start/stop controls. Calculates exact study duration using database timestamps.
7. **Live Room Status**: Periodic auto-refresh (every 8 seconds) and manual refresh for participant counts, active session state, and resources.
8. **Study History**: Student history overview with total focus time calculation (hours and minutes) and breakdown of completed sessions.

---

## 2. Technology Stack & Prerequisites

- **Java**: JDK 17 or higher (compatible up to JDK 26)
- **Build Tool**: Apache Maven 3.8+
- **Database**: MySQL 8.0+
- **Database Driver**: MySQL Connector/J 8.3.0
- **Security**: jBCrypt 0.4
- **Testing**: JUnit 5 (with in-memory H2 for zero-configuration test runs)
- **IDE**: Visual Studio Code (Extension Pack for Java)

---

## 3. Directory Structure

```
FocusRoom/
├── .vscode/
│   └── launch.json                # VS Code launch & debug profiles
├── db.properties.example          # Sample DB configuration template
├── focusroom_schema.sql           # MySQL database schema & seed data
├── pom.xml                        # Maven dependencies & build configuration
├── README.md                      # Comprehensive documentation
└── src/
    ├── main/
    │   ├── java/com/focusroom/
    │   │   ├── dao/
    │   │   │   ├── RoomMemberDAO.java
    │   │   │   ├── StudentDAO.java
    │   │   │   ├── StudyResourceDAO.java
    │   │   │   ├── StudyRoomDAO.java
    │   │   │   └── StudySessionDAO.java
    │   │   ├── model/
    │   │   │   ├── RoomMember.java
    │   │   │   ├── Student.java
    │   │   │   ├── StudyResource.java
    │   │   │   ├── StudyRoom.java
    │   │   │   └── StudySession.java
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   ├── StudyRoomService.java
    │   │   │   └── StudySessionService.java
    │   │   ├── ui/
    │   │   │   ├── DashboardFrame.java
    │   │   │   ├── LoginFrame.java
    │   │   │   ├── RegisterFrame.java
    │   │   │   ├── StudyHistoryFrame.java
    │   │   │   ├── StudyRoomFrame.java
    │   │   │   └── UITheme.java
    │   │   └── Main.java          # Application entry point
    │   └── resources/
    │       └── db.properties      # Active MySQL connection settings
    └── test/
        └── java/com/focusroom/
            └── FocusRoomAppTest.java # 12 Automated JUnit 5 test cases
```

---

## 4. Setup Instructions

### Step 1: Create Database & Execute SQL Script
Open MySQL Command Line Client, MySQL Workbench, or your terminal:

```bash
mysql -u root -p < focusroom_schema.sql
```
Or execute the script in MySQL Workbench / VS Code MySQL extension. This creates the `focusroom` database, all 5 relational tables, constraints, foreign keys, indexes, and sample seed records.

### Step 2: Configure Database Credentials
Edit `src/main/resources/db.properties` (or copy from `db.properties.example`):

```properties
db.url=jdbc:mysql://localhost:3306/focusroom?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

### Step 3: Compile and Run Tests
Run the automated JUnit 5 test suite (all 12 tests run self-contained):

```powershell
mvn clean test
```

### Step 4: Run Application in Terminal
To launch the Swing GUI:

```powershell
mvn compile exec:java -Dexec.mainClass="com.focusroom.Main"
```
Or package and run the self-contained executable JAR:

```powershell
mvn clean package -DskipTests
java -jar target/focusroom-1.0.0.jar
```

---

## 5. Sample Login Credentials

If you loaded `focusroom_schema.sql`, you can immediately test using these pre-seeded accounts:

| Student Name | Email Address | Password |
|---|---|---|
| Alice Johnson | `alice@university.edu` | `Password@123` |
| Bob Smith | `bob@university.edu` | `Password@123` |
| Charlie Davis | `charlie@university.edu` | `Password@123` |
