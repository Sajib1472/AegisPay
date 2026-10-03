package com.aegispay.engine.concierge;

import com.aegispay.engine.AegisPayEngine;
import com.aegispay.engine.EngineOptions;
import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Attestation;
import com.aegispay.engine.model.WorkPeriod.Attestation.MealAnswer;
import com.aegispay.engine.model.WorkPeriod.PayRate;
import com.aegispay.engine.model.WorkPeriod.RateType;
import com.aegispay.engine.model.WorkPeriod.Shift;
import com.aegispay.engine.model.WorkPeriod.TenantPolicy;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.rules.PublishedRulePack;
import com.aegispay.engine.rules.RulePack;
import com.aegispay.engine.time.PunchPairer;
import com.aegispay.engine.time.PunchPairer.RawPunch;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 1.4 concierge: turn a de-identified punch export into a cited autopsy.
 * This is the $150 PDF before anyone pays $299/month.
 */
public final class ConciergeAudit {

    private final AegisPayEngine engine = new AegisPayEngine();
    private final PunchPairer pairer = new PunchPairer();

    public AutopsyReport run(Path employeesFile, Path punchesFile, Path attestationsFile, Path bonusesFile) {
        Map<String, ConciergeEmployee> employees = new LinkedHashMap<>();
        for (String[] cols : Csv.dataRows(employeesFile)) {
            ConciergeEmployee employee = ConciergeEmployee.parse(cols);
            employees.put(employee.code(), employee);
        }
        ConciergePunchFile punches = ConciergePunchFile.parse(Csv.dataRows(punchesFile), employees);
        Map<String, List<Attestation>> attestations = loadAttestations(attestationsFile);
        Map<String, List<WorkPeriod.Bonus>> bonuses = loadBonuses(bonusesFile);

        List<RulePack> packs = List.of(PublishedRulePack.flsa(), PublishedRulePack.california());
        EngineOptions options = EngineOptions.fromPacks(packs);

        List<AutopsyReport.PersonSection> sections = new ArrayList<>();
        Money naiveTotal = Money.ZERO;
        Money engineTotal = Money.ZERO;
        Money premiumTotal = Money.ZERO;
        Money overtimeTotal = Money.ZERO;
        int exceptionCount = 0;

        for (ConciergeEmployee employee : employees.values()) {
            List<RawPunch> raw = punches.byEmployee().getOrDefault(employee.code(), List.of());
            var paired = pairer.pair(raw);
            Shift shift = new Shift(
                    employee.code() + "-loc",
                    employee.jobCode(),
                    employee.timeZone(),
                    employee.jurisdictions(),
                    paired.intervals(),
                    List.of(new PayRate(employee.jobCode(), RateType.HOURLY, employee.hourly()))
            );
            Hours worked = Hours.ZERO;
            for (var interval : paired.intervals()) {
                if (interval.type() == WorkPeriod.IntervalType.WORK
                        || interval.type() == WorkPeriod.IntervalType.PAID_BREAK
                        || interval.type() == WorkPeriod.IntervalType.TRAVEL) {
                    worked = worked.plus(Hours.fromDuration(java.time.Duration.between(interval.start(), interval.end())));
                }
            }
            WorkPeriod period = new WorkPeriod(
                    employee.code(),
                    employee.exemption(),
                    DayOfWeek.SUNDAY,
                    paired.intervals().isEmpty() ? List.of() : List.of(shift),
                    bonuses.getOrDefault(employee.code(), List.of()),
                    attestations.getOrDefault(employee.code(), List.of()),
                    TenantPolicy.conservativeDefaults()
            );
            EarningsResult result = engine.calculate(period, packs, options);
            Money naive = employee.hourly().times(worked);
            naiveTotal = naiveTotal.plus(naive);
            engineTotal = engineTotal.plus(result.totals().gross());
            premiumTotal = premiumTotal.plus(result.totals().premiumPay());
            overtimeTotal = overtimeTotal.plus(result.totals().overtimePay());
            exceptionCount += result.exceptions().size() + paired.problems().size();
            sections.add(new AutopsyReport.PersonSection(
                    employee,
                    result,
                    naive,
                    result.totals().gross().minus(naive),
                    paired.problems()
            ));
        }

        return new AutopsyReport(
                employeesFile.getFileName().toString(),
                punchesFile.getFileName().toString(),
                sections,
                naiveTotal,
                engineTotal,
                engineTotal.minus(naiveTotal),
                premiumTotal,
                overtimeTotal,
                exceptionCount
        );
    }

    private static Map<String, List<Attestation>> loadAttestations(Path path) {
        Map<String, List<Attestation>> map = new LinkedHashMap<>();
        if (path == null) {
            return map;
        }
        for (String[] cols : Csv.dataRows(path)) {
            if (cols.length < 4) {
                continue;
            }
            map.computeIfAbsent(cols[0].trim(), k -> new ArrayList<>()).add(
                    new Attestation(
                            LocalDate.parse(cols[1].trim()),
                            MealAnswer.valueOf(cols[2].trim().toUpperCase()),
                            MealAnswer.valueOf(cols[3].trim().toUpperCase())
                    )
            );
        }
        return map;
    }

    private static Map<String, List<WorkPeriod.Bonus>> loadBonuses(Path path) {
        Map<String, List<WorkPeriod.Bonus>> map = new LinkedHashMap<>();
        if (path == null) {
            return map;
        }
        for (String[] cols : Csv.dataRows(path)) {
            if (cols.length < 4) {
                continue;
            }
            map.computeIfAbsent(cols[0].trim(), k -> new ArrayList<>()).add(
                    new WorkPeriod.Bonus(
                            Money.of(cols[1].trim()),
                            LocalDate.parse(cols[2].trim()),
                            Boolean.parseBoolean(cols[3].trim()),
                            cols.length > 4 ? cols[4].trim() : ""
                    )
            );
        }
        return map;
    }

    public record AutopsyReport(
            String employeeFile,
            String punchFile,
            List<PersonSection> people,
            Money naiveStraightTime,
            Money engineGross,
            Money dollarsAtRisk,
            Money premiums,
            Money overtime,
            int exceptionCount
    ) {
        public boolean foundMispayment() {
            return !dollarsAtRisk.isZero()
                    || people.stream().anyMatch(p -> p.result().lines().stream()
                    .anyMatch(l -> l.bucket() == EarningBucket.MEAL_PREMIUM || l.bucket() == EarningBucket.REST_PREMIUM));
        }

        public record PersonSection(
                ConciergeEmployee employee,
                EarningsResult result,
                Money naivePay,
                Money delta,
                List<String> pairingProblems
        ) {
        }
    }
}
