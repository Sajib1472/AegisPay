package com.aegispay.engine.concierge;

import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod.ExemptionStatus;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public record ConciergeEmployee(
        String code,
        String label,
        Money hourly,
        ExemptionStatus exemption,
        List<String> jurisdictions,
        ZoneId timeZone,
        String jobCode
) {
    public static ConciergeEmployee parse(String[] cols) {
        if (cols.length < 6) {
            throw new IllegalArgumentException("employees.csv needs employee_code,label,hourly_rate,exemption,jurisdictions,timezone");
        }
        List<String> jurisdictions = Arrays.stream(cols[4].split("[|+]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        String job = cols.length > 6 && !cols[6].isBlank() ? cols[6].trim() : "STAFF";
        return new ConciergeEmployee(
                cols[0].trim(),
                cols[1].trim(),
                Money.of(cols[2].trim()),
                ExemptionStatus.valueOf(cols[3].trim().toUpperCase()),
                jurisdictions,
                ZoneId.of(cols[5].trim()),
                job
        );
    }
}
