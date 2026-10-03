package com.aegispay.app.shadow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCsvDiffTest {

    @Test
    void excelMissingDailyOtShowsLowerByTheGap() {
        String excel = "employee_code,earning_type,hours,amount\n1002,Regular Hours,8,240.00\n";
        String ours = "employee_code,earning_type,hours,amount\n1002,Regular Hours,8,240.00\n1002,Overtime,1,45.00\n";
        List<ShadowCsvDiff.Row> rows = ShadowCsvDiff.compare(excel, ours);
        assertTrue(rows.stream().anyMatch(r -> r.note().contains("AegisPay booked this line")));
        assertEquals(1, rows.size());
    }
}
