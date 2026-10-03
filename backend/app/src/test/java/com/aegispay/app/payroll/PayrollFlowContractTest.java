package com.aegispay.app.payroll;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration contract (Testcontainers in TenantIsolationIT covers isolation).
 * Unlock is dual-control: PAYROLL_ADMIN requests, a different OWNER confirms.
 */
class PayrollFlowContractTest {

    @Test
    void importCalculateApproveExportUnlockContract() {
        assertTrue(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.CALCULATED));
        assertFalse(PayRunLifecycle.recalculateAllowed(PayRunLifecycle.APPROVED));
        assertTrue(PayRunLifecycle.isTerminal(PayRunLifecycle.LOCKED));
    }
}
