package com.aegispay.app.payroll;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowComparisonServiceTest {

    @Test
    void reportsTheGapWhenExcelMissedDailyOt() {
        String theirs = "employee_code,earning_type,hours,amount\nHD-002,Regular Hours,9,270.00\n";
        String ours = "employee_code,earning_type,hours,amount\nHD-002,Regular Hours,8,240.00\nHD-002,Overtime,1,45.00\n";
        List<ShadowComparisonService.DiffRow> rows = new ShadowComparisonService().diff(theirs, ours);
        assertEquals(1, rows.stream().filter(r -> r.key().contains("Overtime")).count());
        assertEquals(new BigDecimal("45.00"), rows.stream()
                .filter(r -> r.key().contains("Overtime"))
                .findFirst()
                .orElseThrow()
                .delta());
    }
}
