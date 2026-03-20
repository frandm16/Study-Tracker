package com.frandm.pomodoro.database;

import java.io.File;

public class DatabaseConfig {
    public static String getNasUrl() { return System.getenv("ST_NAS_URL"); }
    public static String getNasUser() { return System.getenv("ST_NAS_USER"); }
    public static String getNasPass() { return System.getenv("ST_NAS_PASS"); }

    public static String getLocalSqlitePath() {
        String userHome = System.getProperty("user.home");
        File dbFile = new File(userHome + "/.StudyTracker", "StudyTrackerDatabase.db");
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }
}