package com.aegispay.engine.concierge;

import com.aegispay.engine.time.PunchPairer.PunchKind;
import com.aegispay.engine.time.PunchPairer.RawPunch;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ConciergePunchFile(Map<String, List<RawPunch>> byEmployee) {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ConciergePunchFile parse(List<String[]> rows, Map<String, ConciergeEmployee> employees) {
        Map<String, List<RawPunch>> map = new LinkedHashMap<>();
        for (String[] cols : rows) {
            if (cols.length < 3) {
                continue;
            }
            String code = cols[0].trim();
            ConciergeEmployee employee = employees.get(code);
            if (employee == null) {
                throw new IllegalArgumentException("Punch for unknown employee_code " + code);
            }
            Instant at = parseTimestamp(cols[1].trim(), employee.timeZone());
            PunchKind kind = PunchKind.valueOf(cols[2].trim().toUpperCase());
            String location = cols.length > 3 ? cols[3].trim() : "loc";
            map.computeIfAbsent(code, k -> new ArrayList<>())
                    .add(new RawPunch(at, kind, location, employee.jobCode()));
        }
        return new ConciergePunchFile(map);
    }

    static Instant parseTimestamp(String raw, ZoneId zone) {
        if (raw.contains("T") || raw.endsWith("Z")) {
            return Instant.parse(raw.replace(" ", "T"));
        }
        return LocalDateTime.parse(raw, LOCAL).atZone(zone).toInstant();
    }

    public static LocalDate localDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}
