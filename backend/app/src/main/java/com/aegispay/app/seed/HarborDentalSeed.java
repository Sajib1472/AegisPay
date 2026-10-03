package com.aegispay.app.seed;

import com.aegispay.app.billing.Subscription;
import com.aegispay.app.billing.SubscriptionRepository;
import com.aegispay.app.org.Assignment;
import com.aegispay.app.org.AssignmentRepository;
import com.aegispay.app.org.CompensationPlan;
import com.aegispay.app.org.CompensationPlanRepository;
import com.aegispay.app.org.Employment;
import com.aegispay.app.org.EmploymentRepository;
import com.aegispay.app.org.JobCode;
import com.aegispay.app.org.JobCodeRepository;
import com.aegispay.app.org.Location;
import com.aegispay.app.org.LocationRepository;
import com.aegispay.app.org.LeavePolicy;
import com.aegispay.app.org.LeavePolicyRepository;
import com.aegispay.app.org.PayRate;
import com.aegispay.app.org.PayRateRepository;
import com.aegispay.app.org.Person;
import com.aegispay.app.org.PersonRepository;
import com.aegispay.app.org.TenantPolicyEntity;
import com.aegispay.app.org.TenantPolicyRepository;
import com.aegispay.app.payroll.BonusEntry;
import com.aegispay.app.payroll.BonusEntryRepository;
import com.aegispay.app.payroll.PayPeriod;
import com.aegispay.app.payroll.PayPeriodRepository;
import com.aegispay.app.platform.identity.AppUser;
import com.aegispay.app.platform.identity.AppUserRepository;
import com.aegispay.app.platform.identity.Tenant;
import com.aegispay.app.platform.identity.TenantRepository;
import com.aegispay.app.platform.identity.UserRole;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.time.MealAttestation;
import com.aegispay.app.time.MealAttestationRepository;
import com.aegispay.app.time.Punch;
import com.aegispay.app.time.PunchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("local")
public class HarborDentalSeed implements CommandLineRunner {

    private final boolean seedEnabled;
    private final String ownerEmail;
    private final String ownerPassword;
    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final SubscriptionRepository subscriptions;
    private final PasswordEncoder passwordEncoder;
    private final LocationRepository locations;
    private final JobCodeRepository jobs;
    private final PersonRepository people;
    private final EmploymentRepository employments;
    private final AssignmentRepository assignments;
    private final PayRateRepository rates;
    private final CompensationPlanRepository compensationPlans;
    private final LeavePolicyRepository leavePolicies;
    private final TenantPolicyRepository policies;
    private final PunchRepository punches;
    private final MealAttestationRepository attestations;
    private final BonusEntryRepository bonuses;
    private final PayPeriodRepository periods;

    public HarborDentalSeed(
            @Value("${aegispay.seed:false}") boolean seedEnabled,
            @Value("${AEGISPAY_SEED_OWNER_EMAIL:owner@harbordental.example}") String ownerEmail,
            @Value("${AEGISPAY_SEED_OWNER_PASSWORD:HarborDental!demo}") String ownerPassword,
            TenantRepository tenants,
            AppUserRepository users,
            SubscriptionRepository subscriptions,
            PasswordEncoder passwordEncoder,
            LocationRepository locations,
            JobCodeRepository jobs,
            PersonRepository people,
            EmploymentRepository employments,
            AssignmentRepository assignments,
            PayRateRepository rates,
            CompensationPlanRepository compensationPlans,
            LeavePolicyRepository leavePolicies,
            TenantPolicyRepository policies,
            PunchRepository punches,
            MealAttestationRepository attestations,
            BonusEntryRepository bonuses,
            PayPeriodRepository periods
    ) {
        this.seedEnabled = seedEnabled;
        this.ownerEmail = ownerEmail;
        this.ownerPassword = ownerPassword;
        this.tenants = tenants;
        this.users = users;
        this.subscriptions = subscriptions;
        this.passwordEncoder = passwordEncoder;
        this.locations = locations;
        this.jobs = jobs;
        this.people = people;
        this.employments = employments;
        this.assignments = assignments;
        this.rates = rates;
        this.compensationPlans = compensationPlans;
        this.leavePolicies = leavePolicies;
        this.policies = policies;
        this.punches = punches;
        this.attestations = attestations;
        this.bonuses = bonuses;
        this.periods = periods;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || tenants.findBySlug("harbor-dental-group").isPresent()) {
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setLegalName("Harbor Dental Group");
        tenant.setSlug("harbor-dental-group");
        tenant.setStatus("ACTIVE");
        tenant.setPlan("GROUP");
        tenant.setVertical("DENTAL");
        tenant.setAuditPackEnabled(true);
        tenants.save(tenant);

        TenantContext.set(tenant.getId(), null, UserRole.OWNER.name(), "seed");

        AppUser owner = new AppUser();
        owner.setTenantId(tenant.getId());
        owner.setEmail(ownerEmail);
        owner.setPasswordHash(passwordEncoder.encode(ownerPassword));
        owner.setDisplayName("Avery Chen");
        owner.setRole(UserRole.OWNER);
        owner.setEmailVerified(true);
        users.save(owner);

        Subscription subscription = new Subscription();
        subscription.setTenantId(tenant.getId());
        subscription.setPlan("GROUP");
        subscription.setStatus("ACTIVE");
        subscriptions.save(subscription);

        TenantPolicyEntity policy = new TenantPolicyEntity();
        policy.setTenantId(tenant.getId());
        policies.save(policy);

        Location la = location("Harbor Dental — Downtown", "400 S Figueroa St", "Los Angeles", "CA", "90071",
                "America/Los_Angeles", List.of("US-FLSA", "US-CA"));
        Location austin = location("Harbor Dental — Austin", "600 Congress Ave", "Austin", "TX", "78701",
                "America/Chicago", List.of("US-FLSA"));
        locations.save(la);
        locations.save(austin);

        JobCode rdh = job("RDH", "Registered Dental Hygienist");
        JobCode rda = job("RDA", "Registered Dental Assistant");
        JobCode front = job("FRONT", "Front office");
        jobs.save(rdh);
        jobs.save(rda);
        jobs.save(front);

        Person maria = person("1001", "Maria Alvarez", "maria@harbordental.example", "NON_EXEMPT");
        Person jordan = person("1002", "Jordan Blake", "jordan@harbordental.example", "NON_EXEMPT");
        Person sam = person("1003", "Sam Patel", "sam@harbordental.example", "NON_EXEMPT");
        Person lee = person("2001", "Dr. Priya Lee", "priya@harbordental.example", "EXEMPT_SALARY");
        Person nina = person("1004", "Nina Cho", "nina@harbordental.example", "NON_EXEMPT");
        Person chris = person("1005", "Chris Nguyen", "chris@harbordental.example", "NON_EXEMPT");
        Person devon = person("1006", "Devon Brooks", "devon@harbordental.example", "NON_EXEMPT");
        Person kai = person("1007", "Kai Ramirez", "kai@harbordental.example", "NON_EXEMPT");
        people.saveAll(List.of(maria, jordan, sam, lee, nina, chris, devon, kai));

        JobCode assist = job("ASSIST", "Dental assistant");
        jobs.save(assist);

        staff(maria, la, rdh, new BigDecimal("42.00"));
        staff(jordan, la, rda, new BigDecimal("28.00"));
        staff(sam, austin, front, new BigDecimal("22.00"));
        staff(lee, la, rdh, new BigDecimal("0.00"));
        staff(nina, la, rda, new BigDecimal("27.00"));
        staff(chris, la, front, new BigDecimal("24.00"));
        staff(devon, austin, assist, new BigDecimal("20.00"));
        staff(kai, austin, rdh, new BigDecimal("40.00"));

        CompensationPlan hygiene = new CompensationPlan();
        hygiene.setPersonId(maria.getId());
        hygiene.setPlanType("PRODUCTION_PERCENT_PRODUCTION");
        hygiene.setPercent(new BigDecimal("0.3300"));
        hygiene.setDiscretionary(false);
        hygiene.setEffectiveFrom(LocalDate.of(2022, 3, 1));
        compensationPlans.save(hygiene);

        LeavePolicy sick = new LeavePolicy();
        sick.setLeaveType("SICK");
        sick.setAccrualMethod("HOURS_WORKED");
        sick.setAccrualRate(new BigDecimal("0.0333"));
        sick.setCapHours(new BigDecimal("80"));
        sick.setCarryoverHours(new BigDecimal("40"));
        sick.setStateOverlay("CA");
        sick.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        leavePolicies.save(sick);

        LocalDate week = LocalDate.of(2024, 6, 3);
        ZoneId pt = ZoneId.of("America/Los_Angeles");
        // Maria: 9.25 hour CA day (daily OT) with a proper meal
        punch(maria, la, week, 8, 0, "IN", pt);
        punch(maria, la, week, 12, 0, "BREAK_START", pt);
        punch(maria, la, week, 12, 30, "BREAK_END", pt);
        punch(maria, la, week, 17, 15, "OUT", pt);
        attest(maria, week, "YES", "YES");

        // Jordan: 8 hours, missed meal (premium)
        punch(jordan, la, week, 8, 0, "IN", pt);
        punch(jordan, la, week, 16, 0, "OUT", pt);
        attest(jordan, week, "NO", "YES");

        // Sam in Texas: 46-hour week for FLSA OT
        ZoneId ct = ZoneId.of("America/Chicago");
        for (int d = 0; d < 4; d++) {
            LocalDate day = week.plusDays(d);
            punch(sam, austin, day, 8, 0, "IN", ct);
            punch(sam, austin, day, 12, 0, "BREAK_START", ct);
            punch(sam, austin, day, 12, 30, "BREAK_END", ct);
            punch(sam, austin, day, 18, 0, "OUT", ct);
        }
        punch(sam, austin, week.plusDays(4), 8, 0, "IN", ct);
        punch(sam, austin, week.plusDays(4), 18, 0, "OUT", ct);

        // Week 2: Saturday differential (Nina), extra staff, and a second hygienist week
        LocalDate week2 = week.plusDays(7);
        punch(nina, la, week.plusDays(5), 8, 0, "IN", pt);
        punch(nina, la, week.plusDays(5), 12, 0, "BREAK_START", pt);
        punch(nina, la, week.plusDays(5), 12, 30, "BREAK_END", pt);
        punch(nina, la, week.plusDays(5), 16, 0, "OUT", pt);
        attest(nina, week.plusDays(5), "YES", "YES");

        for (int d = 0; d < 5; d++) {
            LocalDate day = week2.plusDays(d);
            punch(chris, la, day, 8, 0, "IN", pt);
            punch(chris, la, day, 12, 0, "BREAK_START", pt);
            punch(chris, la, day, 12, 30, "BREAK_END", pt);
            punch(chris, la, day, 17, 0, "OUT", pt);
            punch(devon, austin, day, 8, 0, "IN", ct);
            punch(devon, austin, day, 17, 0, "OUT", ct);
            punch(kai, austin, day, 8, 0, "IN", ct);
            punch(kai, austin, day, 12, 0, "BREAK_START", ct);
            punch(kai, austin, day, 12, 30, "BREAK_END", ct);
            punch(kai, austin, day, 17, 0, "OUT", ct);
            punch(maria, la, day, 8, 0, "IN", pt);
            punch(maria, la, day, 12, 0, "BREAK_START", pt);
            punch(maria, la, day, 12, 30, "BREAK_END", pt);
            punch(maria, la, day, 17, 0, "OUT", pt);
        }

        BonusEntry bonus = new BonusEntry();
        bonus.setPersonId(maria.getId());
        bonus.setAmount(new BigDecimal("120.00"));
        bonus.setEarnedOn(week.plusDays(4));
        bonus.setDiscretionary(false);
        bonus.setNote("Hygiene production");
        bonuses.save(bonus);

        PayPeriod period = new PayPeriod();
        period.setPeriodType("BIWEEKLY");
        period.setStartDate(week);
        period.setEndDate(week.plusDays(13));
        period.setStatus("OPEN");
        periods.save(period);

        TenantContext.clear();
    }

    private Location location(String name, String line1, String city, String region, String postal,
                              String tz, List<String> jurisdictions) {
        Location location = new Location();
        location.setName(name);
        location.setLine1(line1);
        location.setCity(city);
        location.setRegion(region);
        location.setPostalCode(postal);
        location.setTimeZone(tz);
        location.setJurisdictions(jurisdictions);
        location.setWageOrder("IWC-4");
        location.setOpeningHours(clinicHours());
        return location;
    }

    private static Map<String, Object> clinicHours() {
        Map<String, Object> hours = new LinkedHashMap<>();
        List<List<String>> open = List.of(List.of("08:00", "18:00"));
        hours.put("mon", open);
        hours.put("tue", open);
        hours.put("wed", open);
        hours.put("thu", open);
        hours.put("fri", open);
        hours.put("sat", List.of());
        hours.put("sun", List.of());
        return hours;
    }

    private JobCode job(String code, String name) {
        JobCode job = new JobCode();
        job.setCode(code);
        job.setName(name);
        return job;
    }

    private Person person(String code, String name, String email, String exemption) {
        Person person = new Person();
        person.setExternalEmployeeCode(code);
        person.setLegalName(name);
        person.setEmail(email);
        person.setHireDate(LocalDate.of(2022, 3, 1));
        person.setExemptionStatus(exemption);
        return person;
    }

    private void staff(Person person, Location location, JobCode job, BigDecimal hourly) {
        Employment stint = new Employment();
        stint.setPersonId(person.getId());
        stint.setWorkerType(person.getWorkerType());
        stint.setExemptionStatus(person.getExemptionStatus());
        stint.setHireDate(person.getHireDate());
        stint.setEffectiveFrom(person.getHireDate());
        employments.save(stint);
        Assignment assignment = new Assignment();
        assignment.setPersonId(person.getId());
        assignment.setLocationId(location.getId());
        assignment.setJobCodeId(job.getId());
        assignment.setEffectiveFrom(LocalDate.of(2022, 3, 1));
        assignments.save(assignment);
        PayRate rate = new PayRate();
        rate.setAssignmentId(assignment.getId());
        rate.setRateType("HOURLY");
        rate.setAmount(hourly);
        rate.setEffectiveFrom(LocalDate.of(2022, 3, 1));
        rates.save(rate);
        if ("RDA".equals(job.getCode()) && "1004".equals(person.getExternalEmployeeCode())) {
            PayRate weekend = new PayRate();
            weekend.setAssignmentId(assignment.getId());
            weekend.setRateType("DIFFERENTIAL");
            weekend.setAmount(new BigDecimal("4.00"));
            weekend.setEffectiveFrom(LocalDate.of(2022, 3, 1));
            rates.save(weekend);
        }
    }

    private void punch(Person person, Location location, LocalDate date, int hour, int minute, String type, ZoneId zone) {
        InstantAt at = new InstantAt(date.atTime(hour, minute).atZone(zone).toInstant());
        Punch punch = new Punch();
        punch.setPersonId(person.getId());
        punch.setLocationId(location.getId());
        punch.setPunchType(type);
        punch.setSource("SEED");
        punch.setOriginalAt(at.value);
        punch.setAdjustedAt(at.value);
        punches.save(punch);
    }

    private void attest(Person person, LocalDate date, String meal, String rest) {
        MealAttestation attestation = new MealAttestation();
        attestation.setPersonId(person.getId());
        attestation.setWorkDate(date);
        attestation.setMealReceived(meal);
        attestation.setRestReceived(rest);
        attestation.setSource("SEED");
        attestations.save(attestation);
    }

    private record InstantAt(java.time.Instant value) {
    }
}
