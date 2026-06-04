package com.tihu.frontend.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DISPLAY_FORMATTER);
    }

    public static String formatOr(LocalDateTime time, String fallback) {
        return time == null ? fallback : format(time);
    }

    public static String formatDateTimeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        LocalDateTime parsed = parseDateTimeText(value);
        if (parsed != null) {
            return format(parsed);
        }
        return value.replace('T', ' ');
    }

    private static LocalDateTime parseDateTimeText(String value) {
        String normalized = value.trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized);
        } catch (Exception ex) {
            return null;
        }
    }
}
