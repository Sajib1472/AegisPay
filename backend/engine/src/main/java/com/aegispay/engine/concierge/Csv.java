package com.aegispay.engine.concierge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class Csv {
    private Csv() {
    }

    static List<String[]> dataRows(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] cols = split(line);
                if (i == 0 && looksLikeHeader(cols)) {
                    continue;
                }
                rows.add(cols);
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read " + path, e);
        }
    }

    static String[] split(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                cols.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cols.add(current.toString());
        return cols.toArray(String[]::new);
    }

    private static boolean looksLikeHeader(String[] cols) {
        String first = cols[0].toLowerCase();
        return first.contains("employee") || first.contains("code");
    }
}
