package com.aegispay.app.platform.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForbiddenHrColumnGuardTest {

    @Test
    void punchCsvWithoutSsnIsFine() {
        assertDoesNotThrow(() -> ForbiddenHrColumnGuard.assertSafeCsv(
                "employee_code,timestamp,type,location\n1001,2024-06-03 08:00,IN,dt\n"));
    }

    @Test
    void ssnHeaderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ForbiddenHrColumnGuard.assertSafeCsv(
                "employee_code,ssn,timestamp\n1001,123-45-6789,2024-06-03 08:00\n"));
    }
}
