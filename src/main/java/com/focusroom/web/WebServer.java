package com.focusroom.web;

import com.focusroom.model.*;
import com.focusroom.service.AuthService;
import com.focusroom.service.StudyRoomService;
import com.focusroom.service.StudySessionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded HTTP Web Server providing REST APIs and serving the FocusRoom Single Page Application.
 * Accessible at http://localhost:8080
 */
public class WebServer {
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());
    private final int port;
    private HttpServer server;

    private final AuthService authService;
    private final StudyRoomService roomService;
    private final StudySessionService sessionService;

    public WebServer(int port) {
        this.port = port;
        this.authService = new AuthService();
        this.roomService = new StudyRoomService();
        this.sessionService = new StudySessionService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        // API routes
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/rooms", new RoomsHandler());
        server.createContext("/api/student/history", new StudentHistoryHandler());
        server.createContext("/api/student/stats", new StudentStatsHandler());

        server.start();
        LOGGER.info("FocusRoom Web Server successfully started at http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ==========================================
    // HTTP Utility Methods
    // ==========================================

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, JSONObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject err = new JSONObject();
        err.put("success", false);
        err.put("error", message);
        sendJsonResponse(exchange, statusCode, err);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    // ==========================================
    // Static Files Handler
    // ==========================================
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Look in classpath first, then local files fallback
            byte[] fileBytes = null;
            String resourcePath = "/web" + path;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    fileBytes = is.readAllBytes();
                }
            } catch (Exception ignored) {}

            if (fileBytes == null) {
                File localFile = new File("src/main/resources/web" + path);
                if (localFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(localFile)) {
                        fileBytes = fis.readAllBytes();
                    }
                }
            }

            if (fileBytes == null) {
                String notFound = "<h1>404 Not Found: " + path + "</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String mimeType = "text/html; charset=UTF-8";
            if (path.endsWith(".css")) mimeType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) mimeType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".png")) mimeType = "image/png";
            else if (path.endsWith(".svg")) mimeType = "image/svg+xml";

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // ==========================================
    // API: Registration
    // ==========================================
    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);

                String name = req.optString("name");
                String email = req.optString("email");
                String password = req.optString("password");
                String department = req.optString("department");
                int year = req.optInt("year", 1);

                Student student = authService.register(name, email, password, department, year);

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("message", "Registration successful! Welcome " + student.getStudentName());
                res.put("student", studentToJson(student));
                sendJsonResponse(exchange, 200, res);

            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Registration error", e);
                sendError(exchange, 500, "Registration failed: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // API: Login
    // ==========================================
    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                JSONObject req = new JSONObject(body);
                String email = req.optString("email");
                String password = req.optString("password");

                Student student = authService.login(email, password);

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("message", "Welcome back, " + student.getStudentName());
                res.put("student", studentToJson(student));
                sendJsonResponse(exchange, 200, res);

            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Login error", e);
                sendError(exchange, 500, "Database error: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // API: Rooms & Sub-actions Router
    // ==========================================
    private class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); // e.g. /api/rooms or /api/rooms/3/join
            String method = exchange.getRequestMethod();

            try {
                // GET /api/rooms - List open rooms with filters
                if (path.equals("/api/rooms") && "GET".equalsIgnoreCase(method)) {
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    String subject = params.get("subject");
                    String exam = params.get("exam");
                    List<StudyRoom> rooms = roomService.getAvailableRooms(subject, exam);

                    JSONArray arr = new JSONArray();
                    for (StudyRoom r : rooms) {
                        arr.put(roomToJson(r));
                    }
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("rooms", arr);
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                // GET /api/rooms/my - Rooms joined by student
                if (path.equals("/api/rooms/my") && "GET".equalsIgnoreCase(method)) {
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    int studentId = Integer.parseInt(params.getOrDefault("studentId", "0"));
                    List<StudyRoom> rooms = roomService.getRoomsForStudent(studentId);

                    JSONArray arr = new JSONArray();
                    for (StudyRoom r : rooms) {
                        arr.put(roomToJson(r));
                    }
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("rooms", arr);
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                // POST /api/rooms - Create Room
                if (path.equals("/api/rooms") && "POST".equalsIgnoreCase(method)) {
                    JSONObject req = new JSONObject(readRequestBody(exchange));
                    String name = req.getString("roomName");
                    String subject = req.getString("subject");
                    String exam = req.getString("examName");
                    String desc = req.optString("description", "");
                    int capacity = req.getInt("maxParticipants");
                    int creatorId = req.getInt("creatorId");

                    StudyRoom created = roomService.createRoom(name, subject, exam, desc, capacity, creatorId);
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("room", roomToJson(created));
                    sendJsonResponse(exchange, 200, res);
                    return;
                }

                // Subpaths: /api/rooms/{id}/...
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    int roomId = Integer.parseInt(parts[3]);

                    // GET /api/rooms/{id}
                    if (parts.length == 4 && "GET".equalsIgnoreCase(method)) {
                        Optional<StudyRoom> rOpt = roomService.getRoomDetails(roomId);
                        if (rOpt.isEmpty()) {
                            sendError(exchange, 404, "Room not found");
                            return;
                        }
                        JSONObject res = new JSONObject();
                        res.put("success", true);
                        res.put("room", roomToJson(rOpt.get()));
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    String action = parts[4];

                    // POST /api/rooms/{id}/join
                    if ("join".equalsIgnoreCase(action) && "POST".equalsIgnoreCase(method)) {
                        JSONObject req = new JSONObject(readRequestBody(exchange));
                        int studentId = req.getInt("studentId");
                        boolean ok = roomService.joinRoom(roomId, studentId);
                        JSONObject res = new JSONObject();
                        res.put("success", ok);
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    // POST /api/rooms/{id}/leave
                    if ("leave".equalsIgnoreCase(action) && "POST".equalsIgnoreCase(method)) {
                        JSONObject req = new JSONObject(readRequestBody(exchange));
                        int studentId = req.getInt("studentId");
                        boolean ok = roomService.leaveRoom(roomId, studentId);
                        JSONObject res = new JSONObject();
                        res.put("success", ok);
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    // POST /api/rooms/{id}/close
                    if ("close".equalsIgnoreCase(action) && "POST".equalsIgnoreCase(method)) {
                        JSONObject req = new JSONObject(readRequestBody(exchange));
                        int studentId = req.getInt("studentId");
                        boolean ok = roomService.closeRoom(roomId, studentId);
                        JSONObject res = new JSONObject();
                        res.put("success", ok);
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    // GET /api/rooms/{id}/members
                    if ("members".equalsIgnoreCase(action) && "GET".equalsIgnoreCase(method)) {
                        List<RoomMember> members = roomService.getActiveMembers(roomId);
                        JSONArray arr = new JSONArray();
                        for (RoomMember m : members) {
                            JSONObject mo = new JSONObject();
                            mo.put("studentId", m.getStudentId());
                            mo.put("studentName", m.getStudentName());
                            mo.put("department", m.getDepartment());
                            mo.put("year", m.getYearOfStudy());
                            mo.put("status", m.getParticipantStatus());
                            arr.put(mo);
                        }
                        JSONObject res = new JSONObject();
                        res.put("success", true);
                        res.put("members", arr);
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    // /api/rooms/{id}/resources (GET or POST)
                    if ("resources".equalsIgnoreCase(action)) {
                        if ("GET".equalsIgnoreCase(method)) {
                            List<StudyResource> resources = roomService.getResources(roomId);
                            JSONArray arr = new JSONArray();
                            for (StudyResource r : resources) {
                                JSONObject ro = new JSONObject();
                                ro.put("resourceId", r.getResourceId());
                                ro.put("title", r.getResourceTitle());
                                ro.put("link", r.getResourceLink());
                                ro.put("studentName", r.getStudentName());
                                arr.put(ro);
                            }
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            res.put("resources", arr);
                            sendJsonResponse(exchange, 200, res);
                            return;
                        } else if ("POST".equalsIgnoreCase(method)) {
                            JSONObject req = new JSONObject(readRequestBody(exchange));
                            int studentId = req.getInt("studentId");
                            String title = req.getString("title");
                            String link = req.getString("link");
                            StudyResource created = roomService.shareResource(roomId, studentId, title, link);
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            JSONObject ro = new JSONObject();
                            ro.put("resourceId", created.getResourceId());
                            ro.put("title", created.getResourceTitle());
                            ro.put("link", created.getResourceLink());
                            res.put("resource", ro);
                            sendJsonResponse(exchange, 200, res);
                            return;
                        }
                    }

                    // /api/rooms/{id}/session
                    if ("session".equalsIgnoreCase(action)) {
                        if ("GET".equalsIgnoreCase(method)) {
                            Optional<StudySession> activeOpt = sessionService.getActiveSession(roomId);
                            JSONObject res = new JSONObject();
                            res.put("success", true);
                            if (activeOpt.isPresent()) {
                                res.put("active", true);
                                res.put("session", sessionToJson(activeOpt.get()));
                            } else {
                                res.put("active", false);
                            }
                            sendJsonResponse(exchange, 200, res);
                            return;
                        }
                    }

                    // POST /api/rooms/{id}/session-start
                    if ("session-start".equalsIgnoreCase(action) && "POST".equalsIgnoreCase(method)) {
                        JSONObject req = new JSONObject(readRequestBody(exchange));
                        int studentId = req.getInt("studentId");
                        StudySession session = sessionService.startSession(roomId, studentId);
                        JSONObject res = new JSONObject();
                        res.put("success", true);
                        res.put("session", sessionToJson(session));
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }

                    // POST /api/rooms/{id}/session-stop
                    if ("session-stop".equalsIgnoreCase(action) && "POST".equalsIgnoreCase(method)) {
                        JSONObject req = new JSONObject(readRequestBody(exchange));
                        int sessionId = req.getInt("sessionId");
                        int studentId = req.getInt("studentId");
                        StudySession completed = sessionService.stopSession(sessionId, studentId);
                        JSONObject res = new JSONObject();
                        res.put("success", true);
                        res.put("durationMinutes", completed.getDurationMinutes());
                        sendJsonResponse(exchange, 200, res);
                        return;
                    }
                }

                sendError(exchange, 404, "Endpoint not found: " + path);
            } catch (IllegalArgumentException | IllegalStateException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing " + path, e);
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // ==========================================
    // API: Student History
    // ==========================================
    private class StudentHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int studentId = Integer.parseInt(params.getOrDefault("studentId", "0"));

                List<StudySession> list = sessionService.getStudentHistory(studentId);
                JSONArray arr = new JSONArray();
                for (StudySession s : list) {
                    arr.put(sessionToJson(s));
                }
                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("history", arr);
                sendJsonResponse(exchange, 200, res);
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // ==========================================
    // API: Student Stats
    // ==========================================
    private class StudentStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int studentId = Integer.parseInt(params.getOrDefault("studentId", "0"));

                int totalMinutes = sessionService.getTotalStudyMinutes(studentId);
                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("totalMinutes", totalMinutes);
                res.put("hours", totalMinutes / 60);
                res.put("minutes", totalMinutes % 60);
                sendJsonResponse(exchange, 200, res);
            } catch (Exception e) {
                sendError(exchange, 500, e.getMessage());
            }
        }
    }

    // ==========================================
    // JSON Helpers
    // ==========================================
    private JSONObject studentToJson(Student s) {
        JSONObject o = new JSONObject();
        o.put("studentId", s.getStudentId());
        o.put("name", s.getStudentName());
        o.put("email", s.getEmail());
        o.put("department", s.getDepartment());
        o.put("year", s.getYearOfStudy());
        return o;
    }

    private JSONObject roomToJson(StudyRoom r) {
        JSONObject o = new JSONObject();
        o.put("roomId", r.getRoomId());
        o.put("roomName", r.getRoomName());
        o.put("subject", r.getSubject());
        o.put("examName", r.getExamName());
        o.put("description", r.getRoomDescription());
        o.put("maxParticipants", r.getMaxParticipants());
        o.put("activeCount", r.getActiveParticipantCount());
        o.put("hasActiveSession", r.hasActiveSession());
        o.put("roomStatus", r.getRoomStatus());
        o.put("creatorName", r.getCreatorName());
        o.put("createdBy", r.getCreatedBy());
        return o;
    }

    private JSONObject sessionToJson(StudySession s) {
        JSONObject o = new JSONObject();
        o.put("sessionId", s.getSessionId());
        o.put("roomId", s.getRoomId());
        o.put("roomName", s.getRoomName());
        o.put("subject", s.getSubject());
        o.put("startedBy", s.getStartedBy());
        o.put("startedByName", s.getStartedByName());
        o.put("startTime", s.getStartTime() != null ? s.getStartTime().getTime() : 0);
        o.put("durationMinutes", s.getDurationMinutes());
        o.put("status", s.getSessionStatus());
        return o;
    }
}
