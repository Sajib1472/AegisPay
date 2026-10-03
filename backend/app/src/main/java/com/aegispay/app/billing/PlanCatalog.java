package com.aegispay.app.billing;

import java.util.List;

public final class PlanCatalog {

    private PlanCatalog() {
    }

    public static List<CatalogEntry> entries() {
        return List.of(
                entry(PlanCode.PILOT),
                entry(PlanCode.GROUP),
                entry(PlanCode.NETWORK)
        );
    }

    public static CatalogEntry entry(PlanCode plan) {
        PlanCode.PlanLimits limits = plan.limits();
        return new CatalogEntry(
                plan.name(),
                plan.displayName(),
                plan.who(),
                limits.monthlyCents(),
                limits.trialDays(),
                limits.maxLocations(),
                limits.maxEmployees(),
                limits.maxPayPeriods() == Integer.MAX_VALUE ? null : limits.maxPayPeriods(),
                limits.includedRulePacks(),
                limits.secondStatePack(),
                limits.support(),
                PlanCode.AUDIT_PACK_CENTS,
                "Attorney-ready PDF with statute citations and punch-level math"
        );
    }

    public record CatalogEntry(
            String code,
            String name,
            String who,
            int monthlyCents,
            int trialDays,
            int maxLocations,
            int maxEmployees,
            Integer maxPayPeriods,
            List<String> includedRulePacks,
            boolean secondStatePack,
            String support,
            int auditPackCents,
            String auditPackBlurb
    ) {
    }
}
