package com.frandm.studytracker.database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MigrationManager {

    public static void checkAndMigrate() {
        try (Connection nasConn = ConnectionFactory.getConnection()) {
            if (!ConnectionFactory.isPostgres(nasConn)) return;

            try (Statement st = nasConn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT count(*) FROM tags")) {
                if (rs.next() && rs.getInt(1) > 0) return; // Ya hay datos
            }

            performMigration(nasConn);

        } catch (SQLException e) {
            System.err.println("Error en chequeo de migración: " + e.getMessage());
        }
    }

    private static void performMigration(Connection nasConn) throws SQLException {
        String sqliteUrl = DatabaseConfig.getLocalSqlitePath();

        try (Connection localConn = DriverManager.getConnection(sqliteUrl)) {
            nasConn.setAutoCommit(false);

            //  Migrar TAGS
            Map<Integer, Integer> tagIdMap = new HashMap<>();
            try (Statement st = localConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM tags")) {
                String sql = "INSERT INTO tags (name, color, is_archived, weekly_goal_min) VALUES (?, ?, ?, ?) RETURNING id";
                while (rs.next()) {
                    int oldId = rs.getInt("id");
                    try (PreparedStatement pst = nasConn.prepareStatement(sql)) {
                        pst.setString(1, rs.getString("name"));
                        pst.setString(2, rs.getString("color"));
                        pst.setInt(3, rs.getInt("is_archived"));
                        pst.setInt(4, rs.getInt("weekly_goal_min"));
                        ResultSet res = pst.executeQuery();
                        if (res.next()) tagIdMap.put(oldId, res.getInt(1));
                    }
                }
            }

            //  Migrar TASKS
            Map<Integer, Integer> taskIdMap = new HashMap<>();
            try (Statement st = localConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM tasks")) {
                String sql = "INSERT INTO tasks (tag_id, name, is_favorite, weekly_goal_min) VALUES (?, ?, ?, ?) RETURNING id";
                while (rs.next()) {
                    int oldId = rs.getInt("id");
                    Integer newTagId = tagIdMap.get(rs.getInt("tag_id"));
                    if (newTagId == null) continue;

                    try (PreparedStatement pst = nasConn.prepareStatement(sql)) {
                        pst.setInt(1, newTagId);
                        pst.setString(2, rs.getString("name"));
                        pst.setInt(3, rs.getInt("is_favorite"));
                        pst.setInt(4, rs.getInt("weekly_goal_min"));
                        ResultSet res = pst.executeQuery();
                        if (res.next()) taskIdMap.put(oldId, res.getInt(1));
                    }
                }
            }

            //  Migrar SESSIONS
            try (Statement st = localConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM sessions")) {
                String sql = "INSERT INTO sessions (task_id, title, description, total_minutes, start_date, end_date, rating, is_favorite) VALUES (?,?,?,?,?,?,?,?)";
                while (rs.next()) {
                    Integer newTaskId = taskIdMap.get(rs.getInt("task_id"));
                    if (newTaskId == null) continue;

                    try (PreparedStatement pst = nasConn.prepareStatement(sql)) {
                        pst.setInt(1, newTaskId);
                        pst.setString(2, rs.getString("title"));
                        pst.setString(3, rs.getString("description"));
                        pst.setInt(4, rs.getInt("total_minutes"));

                        // IMPORTANTE: Conversión para evitar errores de tipo en Postgres
                        pst.setTimestamp(5, Timestamp.valueOf(rs.getString("start_date")));
                        pst.setTimestamp(6, Timestamp.valueOf(rs.getString("end_date")));

                        pst.setInt(7, rs.getInt("rating"));
                        pst.setInt(8, rs.getInt("is_favorite"));
                        pst.executeUpdate();
                    }
                }
            }

            //  Migrar SCHEDULED SESSIONS
            try (Statement st = localConn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM scheduled_sessions")) {
                String sql = "INSERT INTO scheduled_sessions (task_id, title, start_time, end_time, is_completed) VALUES (?,?,?,?,?)";
                while (rs.next()) {
                    Integer newTaskId = taskIdMap.get(rs.getInt("task_id"));
                    if (newTaskId != null) {
                        try (PreparedStatement pst = nasConn.prepareStatement(sql)) {
                            pst.setInt(1, newTaskId);
                            pst.setString(2, rs.getString("title"));
                            pst.setTimestamp(3, Timestamp.valueOf(rs.getString("start_time")));
                            pst.setTimestamp(4, Timestamp.valueOf(rs.getString("end_time")));
                            pst.setInt(5, rs.getInt("is_completed"));
                            pst.executeUpdate();
                        }
                    }
                }
            }

            nasConn.commit();
        } catch (Exception e) {
            nasConn.rollback();
            System.err.println(">>> Error MigrationManager.performMigration: " + e.getMessage());
            throw e;
        }
    }
}