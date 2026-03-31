package com.frandm.studytracker.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final DateTimeFormatter API_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtils() {}

    public static LocalDateTime parseApiTimestamp(String value) {
        return LocalDateTime.parse(value, API_TIMESTAMP_FORMAT);
    }

    public static LocalDateTime parseIsoTimestamp(String value) {
        return LocalDateTime.parse(value);
    }

    public static LocalDateTime parseFlexibleTimestamp(String value) {
        return value.contains("T") ? parseIsoTimestamp(value) : parseApiTimestamp(value);
    }

    public static String formatApiTimestamp(LocalDateTime value) {
        return value.format(API_TIMESTAMP_FORMAT);
    }
}
