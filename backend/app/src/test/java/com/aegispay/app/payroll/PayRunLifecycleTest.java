package com.aegispay.app.payroll;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayRunLifecycleTest {

    @Test
    void recalculateStopsAfterApproved() {
        assertTrue(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.DRAFT));
        assertTrue(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.CALCULATED));
        assertTrue(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.EXCEPTIONS_PENDING));
        assertFalse(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.APPROVED));
        assertFalse(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.EXPORTED));
        assertFalse(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.LOCKED));
    }

    @Test
    void blockersSendThePeriodToTheQueue() {
        assertEquals(PayRunLifecycle.EXCEPTIONS_PENDING, PayRunLifecycle.afterCalculate(true));
        assertEquals(PayRunLifecycle.CALCULATED, PayRunLifecycle.afterCalculate(false));
    }

    @Test
    void defaultLockIsTwentyFourHoursAfterExport() {
        Instant exported = Instant.parse("2024-06-10T18:00:00Z");
        assertEquals(exported.plus(PayRunLifecycle.LOCK_AFTER_EXPORT), PayRunLifecycle.lockAt(exported, false));
        assertEquals(exported, PayRunLifecycle.lockAt(exported, true));
        assertTrue(PayRunLifecycle.lockDue(exported, exported));
        assertFalse(PayRunLifecycle.lockDue(exported.plusSeconds(60), exported));
    }
}
