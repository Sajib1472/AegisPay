package com.aegispay.app.domain;

/**
 * Step 4 freeze. Tables exist only if they support a payroll run. geo_claim waits until v1.1.
 */
public final class DomainModel {

    public static final String[] PLATFORM = {
            "platform_user", "tenant", "subscription", "feature_flag", "rule_pack", "rule_pack_document"
    };

    public static final String[] ORG = {
            "location", "person", "employment", "job_code", "assignment", "pay_rate",
            "compensation_plan", "leave_policy"
    };

    public static final String[] TIME = {
            "punch", "punch_import_batch", "timesheet_edit", "meal_attestation"
    };

    public static final String[] PAYROLL = {
            "pay_period", "pay_run", "earnings_line", "pay_run_exception", "export_file", "pay_run_snapshot"
    };

    public static final String[] AUDIT = {"audit_event"};

    public static final boolean PUNCHES_NEVER_DELETED = true;
    public static final boolean EARNINGS_NEVER_DELETED = true;
    public static final boolean SOFT_DELETE_PERSON_LOCATION_ONLY = true;
    public static final boolean RATES_EFFECTIVE_DATED = true;
    public static final boolean TIMESHEET_EDITS_APPEND_ONLY = true;

    private DomainModel() {
    }
}
