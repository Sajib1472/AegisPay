package com.aegispay.app.org;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tenant_policy")
public class TenantPolicyEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "workweek_start", nullable = false)
    private String workweekStart = "SUNDAY";

    @Column(name = "punch_round_minutes", nullable = false)
    private int punchRoundMinutes = 1;

    @Column(name = "meal_waiver_under_six_hours", nullable = false)
    private boolean mealWaiverUnderSixHours = true;

    @Column(name = "auto_rest_premium", nullable = false)
    private boolean autoRestPremium = true;

    @Column(name = "attestation_overrides_clock", nullable = false)
    private boolean attestationOverridesClock = true;

    @Column(name = "pay_period_type", nullable = false)
    private String payPeriodType = "BIWEEKLY";

    @Column(name = "export_destination", nullable = false)
    private String exportDestination = "GUSTO";

    @Column(name = "lock_on_export", nullable = false)
    private boolean lockOnExport;

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getWorkweekStart() {
        return workweekStart;
    }

    public void setWorkweekStart(String workweekStart) {
        this.workweekStart = workweekStart;
    }

    public int getPunchRoundMinutes() {
        return punchRoundMinutes;
    }

    public void setPunchRoundMinutes(int punchRoundMinutes) {
        this.punchRoundMinutes = punchRoundMinutes;
    }

    public boolean isMealWaiverUnderSixHours() {
        return mealWaiverUnderSixHours;
    }

    public boolean isAutoRestPremium() {
        return autoRestPremium;
    }

    public boolean isAttestationOverridesClock() {
        return attestationOverridesClock;
    }

    public String getPayPeriodType() {
        return payPeriodType;
    }

    public String getExportDestination() {
        return exportDestination;
    }

    public boolean isLockOnExport() {
        return lockOnExport;
    }

    public void setLockOnExport(boolean lockOnExport) {
        this.lockOnExport = lockOnExport;
    }
}
