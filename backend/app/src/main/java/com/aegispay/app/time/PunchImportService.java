package com.aegispay.app.time;

import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PunchImportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PunchRepository punches;
    private final PunchImportBatchRepository batches;
    private final PersonRepository people;

    public PunchImportService(
            PunchRepository punches,
            PunchImportBatchRepository batches,
            PersonRepository people
    ) {
        this.punches = punches;
        this.batches = batches;
        this.people = people;
    }

    @Transactional
    public ImportResult importCsv(String fileName, String csv, UUID locationId, ZoneId zone) {
        String sha = sha256(csv);
        UUID tenantId = TenantContext.requireTenantId();
        Optional<PunchImportBatch> existing = batches.findByTenantIdAndFileSha256(tenantId, sha);
        if (existing.isPresent()) {
            return new ImportResult(existing.get().getId(), existing.get().getRowCount(), List.of("Duplicate file; original batch reused"), true);
        }

        PunchImportBatch batch = new PunchImportBatch();
        batch.setFileName(fileName);
        batch.setFileSha256(sha);
        batch.setStatus("COMMITTED");
        batch.setImportedBy(TenantContext.userId());
        batches.save(batch);

        List<String> problems = new ArrayList<>();
        int count = 0;
        String[] lines = csv.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || i == 0 && line.toLowerCase(Locale.ROOT).startsWith("employee")) {
                continue;
            }
            String[] cols = line.split(",", -1);
            if (cols.length < 4) {
                problems.add("Row " + (i + 1) + " does not have employee_code,timestamp,type,location");
                continue;
            }
            String code = cols[0].trim();
            Person person = people.findByTenantIdAndExternalEmployeeCode(tenantId, code).orElse(null);
            if (person == null) {
                problems.add("Unknown employee_code " + code + " on row " + (i + 1));
                continue;
            }
            Instant at = parseTimestamp(cols[1].trim(), zone);
            Punch punch = new Punch();
            punch.setPersonId(person.getId());
            punch.setLocationId(locationId);
            punch.setImportBatchId(batch.getId());
            punch.setPunchType(cols[2].trim().toUpperCase(Locale.ROOT));
            punch.setSource("CSV");
            punch.setOriginalAt(at);
            punch.setAdjustedAt(at);
            punches.save(punch);
            count++;
        }
        batch.setRowCount(count);
        batches.save(batch);
        return new ImportResult(batch.getId(), count, problems, false);
    }

    private static Instant parseTimestamp(String raw, ZoneId zone) {
        if (raw.endsWith("Z") || raw.contains("T")) {
            return Instant.parse(raw.replace(" ", "T"));
        }
        return LocalDateTime.parse(raw, TS).atZone(zone).toInstant();
    }

    private static String sha256(String csv) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(csv.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record ImportResult(UUID batchId, int rowCount, List<String> problems, boolean duplicate) {
    }
}
