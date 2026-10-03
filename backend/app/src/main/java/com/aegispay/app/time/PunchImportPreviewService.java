package com.aegispay.app.time;

import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.engine.time.PunchPairer;
import com.aegispay.engine.time.PunchPairer.PunchKind;
import com.aegispay.engine.time.PunchPairer.RawPunch;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PunchImportPreviewService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final PersonRepository people;
    private final PunchPairer pairer = new PunchPairer();

    public PunchImportPreviewService(PersonRepository people) {
        this.people = people;
    }

    public Preview preview(String csv, String format, Map<String, Integer> mapping, ZoneId zone) {
        com.aegispay.app.platform.security.ForbiddenHrColumnGuard.assertSafeCsv(csv);
        String normalized = ClockCsvFormats.normalize(format, csv, mapping);
        String[] lines = normalized.split("\\R");
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<RawPunch> raw = new ArrayList<>();
        UUID tenantId = TenantContext.requireTenantId();
        int dataRows = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || i == 0) {
                continue;
            }
            dataRows++;
            String[] cols = line.split(",", -1);
            Map<String, String> row = new LinkedHashMap<>();
            row.put("employee_code", cols.length > 0 ? cols[0] : "");
            row.put("timestamp", cols.length > 1 ? cols[1] : "");
            row.put("type", cols.length > 2 ? cols[2] : "");
            row.put("location", cols.length > 3 ? cols[3] : "");
            if (rows.size() < 20) {
                rows.add(row);
            }
            Person person = people.findByTenantIdAndExternalEmployeeCodeAndDeletedAtIsNull(tenantId, row.get("employee_code")).orElse(null);
            if (person == null) {
                problems.add("Unknown employee_code " + row.get("employee_code"));
                continue;
            }
            Instant at = parse(row.get("timestamp"), zone);
            PunchKind kind;
            try {
                kind = PunchKind.valueOf(row.get("type").toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                problems.add("Unknown punch type " + row.get("type"));
                continue;
            }
            raw.add(new RawPunch(at, kind, row.get("location"), "STAFF"));
        }
        var paired = pairer.pair(raw);
        problems.addAll(paired.problems());
        for (var interval : paired.intervals()) {
            Duration length = Duration.between(interval.start(), interval.end());
            if (length.toHours() >= 36) {
                problems.add("36-hour shift at " + interval.start());
            }
        }
        return new Preview(ClockCsvFormats.supported(), rows, problems, dataRows, paired.intervals().size());
    }

    private static Instant parse(String raw, ZoneId zone) {
        if (raw.endsWith("Z") || raw.contains("T")) {
            return Instant.parse(raw.replace(" ", "T"));
        }
        return LocalDateTime.parse(raw, TS).atZone(zone).toInstant();
    }

    public record Preview(List<String> formats, List<Map<String, String>> firstRows, List<String> problems, int rowCount, int intervals) {
    }
}
