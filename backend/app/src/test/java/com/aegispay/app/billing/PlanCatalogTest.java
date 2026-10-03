package com.aegispay.app.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanCatalogTest {

    @Test
    void groupIsTwoNinetyNineAndCapsAtFiveLocations() {
        var group = PlanCode.GROUP.limits();
        assertEquals(299_00, group.monthlyCents());
        assertEquals(5, group.maxLocations());
        assertEquals(80, group.maxEmployees());
        assertTrue(group.includedRulePacks().contains("US-CA"));
        assertFalse(group.secondStatePack());
    }

    @Test
    void pilotHasNoMonthlyPriceAndTwoPeriods() {
        var pilot = PlanCode.PILOT.limits();
        assertEquals(0, pilot.monthlyCents());
        assertEquals(14, pilot.trialDays());
        assertEquals(1, pilot.maxLocations());
        assertEquals(2, pilot.maxPayPeriods());
    }

    @Test
    void auditPackIsNinetyNineAndThereIsNoFreeForeverSku() {
        assertEquals(99_00, PlanCode.AUDIT_PACK_CENTS);
        assertEquals(3, PlanCatalog.entries().size());
        assertTrue(PlanCatalog.entries().stream().noneMatch(e -> e.monthlyCents() == 0 && e.trialDays() == 0));
    }
}
