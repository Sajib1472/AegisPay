package com.aegispay.app.billing;

import java.util.List;

/**
 * Locked v1 catalog. There is no free-forever plan.
 */
public enum PlanCode {
    PILOT,
    GROUP,
    NETWORK;

    public PlanLimits limits() {
        return switch (this) {
            case PILOT -> new PlanLimits(
                    0,
                    14,
                    1,
                    40,
                    2,
                    List.of("US-FLSA", "US-CA"),
                    false,
                    false,
                    "Email, you onboard them yourself"
            );
            case GROUP -> new PlanLimits(
                    299_00,
                    0,
                    5,
                    80,
                    Integer.MAX_VALUE,
                    List.of("US-FLSA", "US-CA"),
                    false,
                    false,
                    "Email support"
            );
            case NETWORK -> new PlanLimits(
                    599_00,
                    0,
                    20,
                    250,
                    Integer.MAX_VALUE,
                    List.of("US-FLSA", "US-CA"),
                    true,
                    true,
                    "Slack/email SLA"
            );
        };
    }

    public String displayName() {
        return switch (this) {
            case PILOT -> "Pilot";
            case GROUP -> "Group";
            case NETWORK -> "Network";
        };
    }

    public String who() {
        return switch (this) {
            case PILOT -> "One group, 14 days, then convert";
            case GROUP -> "2–5 locations";
            case NETWORK -> "6–20 locations";
        };
    }

    public static PlanCode from(String raw) {
        return PlanCode.valueOf(raw.trim().toUpperCase());
    }

    public record PlanLimits(
            int monthlyCents,
            int trialDays,
            int maxLocations,
            int maxEmployees,
            int maxPayPeriods,
            List<String> includedRulePacks,
            boolean secondStatePack,
            boolean ssoLater,
            String support
    ) {
        public int auditPackCents() {
            return AUDIT_PACK_CENTS;
        }
    }

    public static final int AUDIT_PACK_CENTS = 99_00;
}
