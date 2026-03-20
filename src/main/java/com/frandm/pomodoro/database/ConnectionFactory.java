package com.frandm.pomodoro.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public static Connection getConnection() throws SQLException {
        String nasUrl = DatabaseConfig.getNasUrl();

        if (nasUrl != null && !nasUrl.isEmpty()) {
            try {
                DriverManager.setLoginTimeout(2);
                return DriverManager.getConnection(
                        nasUrl,
                        DatabaseConfig.getNasUser(),
                        DatabaseConfig.getNasPass()
                );
            } catch (SQLException e) {
                System.err.println(">>> NAS no alcanzable. Usando SQLite local.");
            }
        }
        return DriverManager.getConnection(DatabaseConfig.getLocalSqlitePath());
    }

    public static boolean isPostgres(Connection conn) throws SQLException {
        return conn.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL");
    }
}