package com.aegispay.engine.concierge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConciergeAuditTest {

    @TempDir
    Path tmp;

    @Test
    void missedMealShowsDollarsAtRisk() throws Exception {
        Path employees = tmp.resolve("employees.csv");
        Path punches = tmp.resolve("punches.csv");
        Path attestations = tmp.resolve("attestations.csv");
        Files.writeString(employees, """
                employee_code,label,hourly_rate,exemption,jurisdictions,timezone,job_code
                1002,Jordan,28.00,NON_EXEMPT,US-FLSA|US-CA,America/Los_Angeles,RDA
                """);
        Files.writeString(punches, """
                employee_code,timestamp,type,location
                1002,2024-06-03 08:00,IN,downtown
                1002,2024-06-03 16:00,OUT,downtown
                """);
        Files.writeString(attestations, """
                employee_code,work_date,meal,rest
                1002,2024-06-03,NO,YES
                """);

        ConciergeAudit.AutopsyReport report = new ConciergeAudit().run(employees, punches, attestations, null);
        assertTrue(report.foundMispayment());
        assertTrue(report.premiums().toBigDecimal().signum() > 0);
        String md = AutopsyRenderer.markdown(report);
        assertTrue(md.contains("Meal"));
        assertTrue(md.contains("226.7"));
    }
}
