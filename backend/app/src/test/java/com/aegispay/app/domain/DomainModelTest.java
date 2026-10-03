package com.aegispay.app.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {

    @Test
    void v1TablesSupportAPayrollRun() {
        assertEquals(6, DomainModel.PLATFORM.length);
        assertEquals(8, DomainModel.ORG.length);
        assertEquals(4, DomainModel.TIME.length);
        assertEquals(6, DomainModel.PAYROLL.length);
        assertEquals(1, DomainModel.AUDIT.length);
        assertTrue(DomainModel.PUNCHES_NEVER_DELETED);
        assertTrue(DomainModel.EARNINGS_NEVER_DELETED);
        assertTrue(DomainModel.SOFT_DELETE_PERSON_LOCATION_ONLY);
        assertTrue(DomainModel.RATES_EFFECTIVE_DATED);
        assertTrue(DomainModel.TIMESHEET_EDITS_APPEND_ONLY);
    }

    @Test
    void wednesdayRaiseDoesNotRewriteMonday() {
        LocalDate wednesday = LocalDate.of(2024, 6, 5);
        assertFalse(EffectiveDating.covers(wednesday, null, LocalDate.of(2024, 6, 3)));
        assertTrue(EffectiveDating.covers(wednesday, null, wednesday));
        assertFalse(EffectiveDating.covers(wednesday, LocalDate.of(2024, 6, 6), LocalDate.of(2024, 6, 7)));
    }
}
