package com.aegispay.app.billing;

import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.platform.identity.Permission;
import com.aegispay.app.platform.identity.Tenant;
import com.aegispay.app.platform.identity.TenantRepository;
import com.aegispay.app.platform.identity.UserRole;
import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EntitlementService {

    private final TenantRepository tenants;
    private final LocationRepository locations;
    private final PersonRepository people;
    private final PayPeriodRepository periods;
    private final TenantEntitlementRepository extras;

    public EntitlementService(
            TenantRepository tenants,
            LocationRepository locations,
            PersonRepository people,
            PayPeriodRepository periods,
            TenantEntitlementRepository extras
    ) {
        this.tenants = tenants;
        this.locations = locations;
        this.people = people;
        this.periods = periods;
        this.extras = extras;
    }

    public AccountView current() {
        Tenant tenant = tenant();
        PlanCode plan = PlanCode.from(tenant.getPlan());
        PlanCode.PlanLimits limits = plan.limits();
        long locationCount = locations.countByTenantIdAndDeletedAtIsNull(tenant.getId());
        long employeeCount = people.countByTenantIdAndDeletedAtIsNull(tenant.getId());
        long periodCount = periods.countByTenantId(tenant.getId());
        UserRole role = UserRole.valueOf(TenantContext.role() == null ? "VIEWER" : TenantContext.role());
        return new AccountView(
                tenant.getLegalName(),
                plan.name(),
                tenant.getStatus(),
                tenant.getTrialEndsAt(),
                tenant.isAuditPackEnabled() || hasExtra("AUDIT_PACK"),
                locationCount,
                employeeCount,
                periodCount,
                limits.maxLocations(),
                limits.maxEmployees(),
                limits.maxPayPeriods() == Integer.MAX_VALUE ? null : limits.maxPayPeriods(),
                limits.includedRulePacks(),
                limits.secondStatePack() || hasExtra("SECOND_STATE_PACK"),
                writable(tenant),
                Permission.forRole(role).stream().map(Enum::name).toList(),
                PlanCatalog.entry(plan),
                List.of(Positioning.VS_GUSTO, Positioning.VS_HOMEBASE, Positioning.VS_HR_FOR_HEALTH, Positioning.VS_WAGEROOT)
        );
    }

    public void assertWritable() {
        Tenant tenant = tenant();
        if (!writable(tenant)) {
            throw new IllegalStateException("Pilot ended. Convert to Group ($299/mo) to keep running payroll.");
        }
    }

    public void assertCanAddLocation() {
        assertWritable();
        AccountView view = current();
        if (view.locationCount() >= view.maxLocations()) {
            throw new IllegalStateException("Location limit reached for the "
                    + view.plan() + " plan (" + view.maxLocations() + "). Upgrade to add another clinic.");
        }
    }

    public void assertCanAddPerson() {
        assertWritable();
        AccountView view = current();
        if (view.employeeCount() >= view.maxEmployees()) {
            throw new IllegalStateException("Employee limit reached for the "
                    + view.plan() + " plan (" + view.maxEmployees() + ").");
        }
    }

    public void assertCanOpenPayPeriod() {
        assertWritable();
        AccountView view = current();
        if (view.maxPayPeriods() != null && view.periodCount() >= view.maxPayPeriods()) {
            throw new IllegalStateException("Pilot includes two pay periods. Convert to Group to keep running payroll.");
        }
    }

    public void assertCanApprove() {
        assertWritable();
        UserRole role = UserRole.valueOf(TenantContext.role());
        if (!Permission.forRole(role).contains(Permission.PAYROLL_APPROVE)) {
            throw new IllegalStateException("PAYROLL_APPROVE is required to sign off a payroll run.");
        }
    }

    public Set<String> extraCodes() {
        return extras.findByTenantId(TenantContext.requireTenantId()).stream()
                .filter(TenantEntitlement::isEnabled)
                .map(TenantEntitlement::getCode)
                .collect(Collectors.toSet());
    }

    private boolean hasExtra(String code) {
        return extraCodes().contains(code);
    }

    private boolean writable(Tenant tenant) {
        if ("READ_ONLY".equals(tenant.getStatus())) {
            return false;
        }
        if (PlanCode.PILOT.name().equals(tenant.getPlan())
                && tenant.getTrialEndsAt() != null
                && tenant.getTrialEndsAt().isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    private Tenant tenant() {
        return tenants.findById(TenantContext.requireTenantId()).orElseThrow();
    }

    public record AccountView(
            String tenantName,
            String plan,
            String status,
            Instant trialEndsAt,
            boolean auditPack,
            long locationCount,
            long employeeCount,
            long periodCount,
            int maxLocations,
            int maxEmployees,
            Integer maxPayPeriods,
            List<String> rulePacks,
            boolean secondStatePack,
            boolean writable,
            List<String> permissions,
            PlanCatalog.CatalogEntry catalog,
            List<String> positioning
    ) {
    }
}
