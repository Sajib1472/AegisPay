package com.aegispay.app.org;

import com.aegispay.app.billing.EntitlementService;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.platform.identity.Tenant;
import com.aegispay.app.platform.identity.TenantRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WizardService {

    private static final List<String> STEPS = List.of(
            "PROFILE", "LOCATIONS", "PAY_PERIOD", "JOB_CODES", "PEOPLE", "CLOCK", "DESTINATION", "POLICIES", "DONE"
    );

    private final TenantRepository tenants;
    private final LocationRepository locations;
    private final JobCodeRepository jobs;
    private final PersonRepository people;
    private final PayPeriodRepository periods;
    private final TenantPolicyRepository policies;
    private final EntitlementService entitlements;
    private final ObjectMapper mapper;

    public WizardService(
            TenantRepository tenants,
            LocationRepository locations,
            JobCodeRepository jobs,
            PersonRepository people,
            PayPeriodRepository periods,
            TenantPolicyRepository policies,
            EntitlementService entitlements,
            ObjectMapper mapper
    ) {
        this.tenants = tenants;
        this.locations = locations;
        this.jobs = jobs;
        this.people = people;
        this.periods = periods;
        this.policies = policies;
        this.entitlements = entitlements;
        this.mapper = mapper;
    }

    public WizardView current() {
        Tenant tenant = tenant();
        return new WizardView(
                tenant.getWizardStep(),
                STEPS,
                tenant.getLegalName(),
                tenant.getVertical(),
                locations.findByTenantIdAndDeletedAtIsNull(tenant.getId()).size(),
                jobs.findByTenantId(tenant.getId()).size(),
                people.findByTenantIdAndDeletedAtIsNull(tenant.getId()).size(),
                tenant.getClockMapping()
        );
    }

    @Transactional
    public WizardView profile(String legalName, String vertical) {
        Tenant tenant = tenant();
        if (legalName != null && !legalName.isBlank()) {
            tenant.setLegalName(legalName);
        }
        if (vertical != null && !vertical.isBlank()) {
            tenant.setVertical(vertical);
        }
        tenant.setWizardStep("LOCATIONS");
        return current();
    }

    public Map<String, Object> previewLocation(String city, String region) {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("timeZone", JurisdictionResolver.inferTimeZone(region));
        preview.put("jurisdictions", JurisdictionResolver.resolve(city, region, "US"));
        preview.put("confirm", "Confirm timezone and jurisdiction chips before saving. Locked periods never silently re-resolve.");
        return preview;
    }

    @Transactional
    public Location addLocation(LocationDraft draft) {
        entitlements.assertCanAddLocation();
        Location location = new Location();
        location.setName(draft.name());
        location.setLine1(draft.line1() == null ? "" : draft.line1());
        location.setCity(draft.city());
        location.setRegion(draft.region());
        location.setPostalCode(draft.postalCode() == null ? "" : draft.postalCode());
        location.setTimeZone(draft.timeZone() == null
                ? JurisdictionResolver.inferTimeZone(draft.region())
                : draft.timeZone());
        location.setJurisdictions(draft.jurisdictions() == null || draft.jurisdictions().isEmpty()
                ? JurisdictionResolver.resolve(draft.city(), draft.region(), "US")
                : draft.jurisdictions());
        Location saved = locations.save(location);
        advance("PAY_PERIOD");
        return saved;
    }

    @Transactional
    public PayPeriod payPeriod(String type, LocalDate start, LocalDate end) {
        entitlements.assertCanOpenPayPeriod();
        PayPeriod period = new PayPeriod();
        period.setPeriodType(type == null ? "BIWEEKLY" : type);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus("DRAFT");
        PayPeriod saved = periods.save(period);
        TenantPolicyEntity policy = policies.findById(TenantContext.requireTenantId()).orElseGet(TenantPolicyEntity::new);
        policy.setTenantId(TenantContext.requireTenantId());
        // payPeriodType has no setter in older entity — skip if missing
        advance("JOB_CODES");
        return saved;
    }

    @Transactional
    public List<JobCode> applyJobTemplate(String vertical) {
        JsonNode root = template(vertical);
        List<JobCode> created = new ArrayList<>();
        for (JsonNode node : root.path("jobCodes")) {
            JobCode job = new JobCode();
            job.setCode(node.path("code").asText());
            job.setName(node.path("name").asText());
            created.add(jobs.save(job));
        }
        advance("PEOPLE");
        return created;
    }

    public JsonNode template(String vertical) {
        String file = switch (vertical == null ? "" : vertical.toUpperCase()) {
            case "PHYSICAL_THERAPY", "PT" -> "templates/vertical-pt.json";
            case "URGENT_CARE" -> "templates/vertical-urgent-care.json";
            case "VETERINARY", "VET" -> "templates/vertical-vet.json";
            default -> "templates/vertical-dental.json";
        };
        try {
            byte[] bytes = new ClassPathResource(file).getInputStream().readAllBytes();
            return mapper.readTree(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Vertical template missing: " + file, e);
        }
    }

    @Transactional
    public Tenant clockMapping(Map<String, Object> mapping) {
        Tenant tenant = tenant();
        tenant.setClockMapping(mapping == null ? Map.of() : mapping);
        tenant.setWizardStep("DESTINATION");
        return tenant;
    }

    @Transactional
    public Tenant destination(String destination) {
        TenantPolicyEntity policy = policies.findById(TenantContext.requireTenantId()).orElseGet(TenantPolicyEntity::new);
        policy.setTenantId(TenantContext.requireTenantId());
        policies.save(policy);
        advance("POLICIES");
        return tenant();
    }

    @Transactional
    public Tenant policies(int roundMinutes, boolean mealWaiver, boolean autoRest) {
        TenantPolicyEntity policy = policies.findById(TenantContext.requireTenantId()).orElseGet(TenantPolicyEntity::new);
        policy.setTenantId(TenantContext.requireTenantId());
        policy.setPunchRoundMinutes(roundMinutes <= 0 ? 1 : roundMinutes);
        policies.save(policy);
        Tenant tenant = tenant();
        tenant.setWizardStep("DONE");
        return tenant;
    }

    private void advance(String step) {
        Tenant tenant = tenant();
        tenant.setWizardStep(step);
    }

    private Tenant tenant() {
        return tenants.findById(TenantContext.requireTenantId()).orElseThrow();
    }

    public record WizardView(
            String step,
            List<String> steps,
            String legalName,
            String vertical,
            int locationCount,
            int jobCodeCount,
            int peopleCount,
            Map<String, Object> clockMapping
    ) {
    }

    public record LocationDraft(
            String name,
            String line1,
            String city,
            String region,
            String postalCode,
            String timeZone,
            List<String> jurisdictions
    ) {
    }
}
