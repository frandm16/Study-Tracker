package com.frandm.pomodoro;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class DatabaseHandler {
    private static final String FOLDER_NAME = ".StudyTracker";
    private static final String DB_NAME = "StudyTrackerDatabase.db";

    public static String getDatabaseUrl() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        if (!configDir.exists()) {
            boolean success = configDir.mkdirs();
            if(!success){
                System.err.println("Error creating config folder");
            }
        }

        File dbFile = new File(configDir, DB_NAME);
        return "jdbc:sqlite:" + dbFile.toPath().toAbsolutePath();
    }

    public static void initializeDatabase() {
        String sqlSessions = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "subject TEXT NOT NULL, " +
                "topic TEXT, " +
                "description TEXT, " +
                "duration_minutes INTEGER NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlSessions);
            System.out.println("[DEBUG] Database initialized correctly");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public static void saveSession(String subject, String topic, String description, int minutes) {
        if(minutes>=0){
            String sql = "INSERT INTO sessions(subject, topic, description, duration_minutes) VALUES(?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
                 PreparedStatement test = conn.prepareStatement(sql)) {

                test.setString(1, subject);
                test.setString(2, topic);
                test.setString(3, description);
                test.setInt(4, minutes);

                test.executeUpdate();
                System.out.println("[DEBUG] Session saved: " + subject + " - " + topic + " - " + description + " - " + minutes + " min");

            } catch (SQLException e) {
                System.err.println("Error saving session: " + e.getMessage());
            }
        }
    }

    public static ObservableList<Session> getAllSessions() {
        ObservableList<Session> sessions = FXCollections.observableArrayList();
        String sql = "SELECT id, subject, topic, description, duration_minutes, timestamp FROM sessions ORDER BY timestamp DESC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getInt("id"),
                        rs.getString("timestamp"),
                        rs.getString("timestamp"),
                        rs.getString("subject"),
                        rs.getString("topic"),
                        rs.getString("description"),
                        rs.getInt("duration_minutes")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllSessions(): " + e.getMessage());
        }
        return sessions;
    }

    public static java.util.Map<java.time.LocalDate, Integer> getMinutesPerDayLastYear() {
        java.util.Map<java.time.LocalDate, Integer> data = new java.util.HashMap<>();
        String sql = "SELECT date(timestamp) as day, SUM(duration_minutes) as total " +
                "FROM sessions " +
                "WHERE timestamp >= date('now', '-1 year') " +
                "GROUP BY day";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                data.put(java.time.LocalDate.parse(rs.getString("day")), rs.getInt("total"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading heatmap: " + e.getMessage());
        }
        return data;
    }

    public static void generateRandomPomodoros() {
        String sqlSession = "INSERT INTO sessions(subject, topic, description, duration_minutes, timestamp) VALUES(?, ?, ?, ?, ?)";
        String sqlEvent = "INSERT INTO session_events(session_id, event_timestamp, event_type) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl())) {
            conn.setAutoCommit(false);

            try (PreparedStatement psSession = conn.prepareStatement(sqlSession, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psEvent = conn.prepareStatement(sqlEvent)) {

                java.util.Random random = new java.util.Random();
                java.time.LocalDate today = java.time.LocalDate.now();
                String[] subjects = {"Programación", "Matemáticas", "Diseño", "Inglés"};

                for (int i = 0; i < 365; i++) {
                    java.time.LocalDate date = today.minusDays(i);

                    if (random.nextDouble() < 0.65) {

                        java.time.LocalDateTime currentTime = date.atTime(6, 0);
                        int sessionsToday = random.nextInt(4) + 1;

                        for (int s = 0; s < sessionsToday; s++) {
                            String subject = subjects[random.nextInt(subjects.length)];

                            int duration = 40 + random.nextInt(51);

                            java.time.LocalDateTime sessionStart = currentTime;
                            java.time.LocalDateTime sessionEnd = sessionStart.plusMinutes(duration);

                            psSession.setString(1, subject);
                            psSession.setString(2, "Tema " + (i+s));
                            psSession.setString(3, "Generado automáticamente");
                            psSession.setInt(4, duration);
                            psSession.setString(5, sessionStart.toString());
                            psSession.executeUpdate();

                            ResultSet rs = psSession.getGeneratedKeys();
                            int sessionId = -1;
                            if (rs.next()) {
                                sessionId = rs.getInt(1);
                            }

                            psEvent.setInt(1, sessionId);

                            psEvent.setString(2, sessionStart.toString());
                            psEvent.setString(3, "started");
                            psEvent.addBatch();

                            if (random.nextDouble() < 0.4) {
                                java.time.LocalDateTime pauseTime = sessionStart.plusMinutes(duration / 2);
                                java.time.LocalDateTime resumeTime = pauseTime.plusMinutes(5);

                                psEvent.setString(2, pauseTime.toString());
                                psEvent.setString(3, "paused");
                                psEvent.addBatch();

                                psEvent.setString(2, resumeTime.toString());
                                psEvent.setString(3, "resumed");
                                psEvent.addBatch();
                            }

                            psEvent.setString(2, sessionEnd.toString());
                            psEvent.setString(3, "finalized");
                            currentTime = sessionEnd.plusMinutes(15);

                            if (currentTime.getHour() >= 22) break;
                        }
                    }
                }
                psEvent.executeBatch();
                conn.commit();
                System.out.println("[DEBUG] 365 días de datos generados secuencialmente.");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    public static List<Session> getSessionsByDate(LocalDate date) {
        List<Session> sessions = new java.util.ArrayList<>();
        String sql = "SELECT * FROM sessions WHERE date(timestamp) = ? ORDER BY timestamp ASC";

        try (Connection conn = DriverManager.getConnection(getDatabaseUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sessions.add(new Session(
                        rs.getInt("id"),
                        rs.getString("timestamp"),
                        rs.getString("timestamp"),
                        rs.getString("subject"),
                        rs.getString("topic"),
                        rs.getString("description"),
                        rs.getInt("duration_minutes")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }
}