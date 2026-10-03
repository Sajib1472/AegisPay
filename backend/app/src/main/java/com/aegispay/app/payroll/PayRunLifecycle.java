package com.aegispay.app.payroll;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class PayRunLifecycle {

    public static final String DRAFT = "DRAFT";
    public static final String CALCULATED = "CALCULATED";
    public static final String EXCEPTIONS_PENDING = "EXCEPTIONS_PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String EXPORTED = "EXPORTED";
    public static final String LOCKED = "LOCKED";

    public static final Duration LOCK_AFTER_EXPORT = Duration.ofHours(24);

    private static final Set<String> TERMINAL = Set.of(APPROVED, EXPORTED, LOCKED);

    private PayRunLifecycle() {
    }

    public static boolean recalculateAllowed(String status) {
        return status == null || !TERMINAL.contains(status);
    }

    public static boolean isTerminal(String status) {
        return status != null && TERMINAL.contains(status);
    }

    public static String afterCalculate(boolean blockers) {
        return blockers ? EXCEPTIONS_PENDING : CALCULATED;
    }

    public static Instant lockAt(Instant exportedAt, boolean lockOnExport) {
        if (exportedAt == null) {
            return null;
        }
        return lockOnExport ? exportedAt : exportedAt.plus(LOCK_AFTER_EXPORT);
    }

    public static boolean lockDue(Instant lockAt, Instant now) {
        return lockAt != null && !now.isBefore(lockAt);
    }
}
