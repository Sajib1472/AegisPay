package com.aegispay.app.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Office managers live in CSV. Map common clock exports onto employee_code,timestamp,type,location.
 */
public final class ClockCsvFormats {

    private ClockCsvFormats() {
    }

    public static List<String> supported() {
        return List.of("GENERIC", "HOMEBASE", "DEPUTY", "TIME_CLOCK_PLUS", "MANUAL_EXCEL");
    }

    public static String normalize(String format, String csv, Map<String, Integer> mapping) {
        String kind = format == null ? "GENERIC" : format.toUpperCase(Locale.ROOT);
        String[] lines = csv.split("\\R");
        if (lines.length == 0) {
            return csv;
        }
        List<String> out = new ArrayList<>();
        out.add("employee_code,timestamp,type,location");
        int start = looksLikeHeader(lines[0]) ? 1 : 0;
        for (int i = start; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cols = lines[i].split(",", -1);
            int code = col(mapping, "employee_code", defaultIndex(kind, 0));
            int ts = col(mapping, "timestamp", defaultIndex(kind, 1));
            int type = col(mapping, "type", defaultIndex(kind, 2));
            int loc = col(mapping, "location", defaultIndex(kind, 3));
            out.add(safe(cols, code) + "," + safe(cols, ts) + "," + mapType(kind, safe(cols, type)) + "," + safe(cols, loc));
        }
        return String.join("\n", out) + "\n";
    }

    private static boolean looksLikeHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("employee") || lower.contains("timestamp") || lower.contains("in/out");
    }

    private static int defaultIndex(String kind, int generic) {
        if ("HOMEBASE".equals(kind) && generic == 2) {
            return 4;
        }
        return generic;
    }

    private static int col(Map<String, Integer> mapping, String name, int fallback) {
        if (mapping != null && mapping.get(name) != null) {
            return mapping.get(name);
        }
        return fallback;
    }

    private static String safe(String[] cols, int index) {
        if (index < 0 || index >= cols.length) {
            return "";
        }
        return cols[index].trim();
    }

    private static String mapType(String kind, String raw) {
        String v = raw.toUpperCase(Locale.ROOT);
        if (v.contains("IN") && !v.contains("OUT")) {
            return "IN";
        }
        if (v.contains("OUT")) {
            return "OUT";
        }
        if (v.contains("BREAK") && v.contains("END")) {
            return "BREAK_END";
        }
        if (v.contains("BREAK") || v.contains("MEAL")) {
            return "BREAK_START";
        }
        return v.isBlank() ? "IN" : v;
    }
}
