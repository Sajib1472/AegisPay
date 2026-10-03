package com.aegispay.engine.concierge;

import com.aegispay.engine.EngineVersion;
import com.aegispay.engine.concierge.ConciergeAudit.AutopsyReport;
import com.aegispay.engine.concierge.ConciergeAudit.AutopsyReport.PersonSection;
import com.aegispay.engine.result.EarningsResult.EarningsLine;

import java.time.LocalDate;
import java.util.StringJoiner;

public final class AutopsyRenderer {

    private AutopsyRenderer() {
    }

    public static String markdown(AutopsyReport report) {
        StringJoiner out = new StringJoiner("\n");
        out.add("# AegisPay payroll autopsy");
        out.add("");
        out.add("_Calculation assistance, not legal advice. You remain the employer of record._");
        out.add("");
        out.add("- Engine: " + EngineVersion.VALUE);
        out.add("- Employees file: " + report.employeeFile());
        out.add("- Punches file: " + report.punchFile());
        out.add("- Date: " + LocalDate.now());
        out.add("");
        out.add("## Dollars at risk");
        out.add("");
        out.add("| | Amount |");
        out.add("|---|---:|");
        out.add("| If you only paid hourly × hours | $" + report.naiveStraightTime() + " |");
        out.add("| Engine gross (straight + OT + premiums + bonuses) | $" + report.engineGross() + " |");
        out.add("| **Delta (likely missed OT / premiums / true-up)** | **$" + report.dollarsAtRisk() + "** |");
        out.add("| Meal/rest/split premiums | $" + report.premiums() + " |");
        out.add("| Overtime dollars | $" + report.overtime() + " |");
        out.add("| Exceptions / pairing problems | " + report.exceptionCount() + " |");
        out.add("");
        out.add(report.foundMispayment()
                ? "**Guarantee check:** at least one under/overpayment pattern was found. The paid pilot is justified."
                : "**Guarantee check:** no delta vs straight time. If this is a real clinic file, the 14-day pilot is free.");
        out.add("");
        for (PersonSection person : report.people()) {
            out.add("## " + person.employee().label() + " (`" + person.employee().code() + "`)");
            out.add("");
            out.add("- Rate: $" + person.employee().hourly() + "/hr · " + person.employee().exemption());
            out.add("- Jurisdictions: " + String.join(", ", person.employee().jurisdictions()));
            out.add("- Naive straight time: $" + person.naivePay());
            out.add("- Engine gross: $" + person.result().totals().gross());
            out.add("- Delta: $" + person.delta());
            out.add("- Regular rate used: $" + person.result().regularRate());
            if (!person.pairingProblems().isEmpty()) {
                out.add("- Pairing problems:");
                for (String problem : person.pairingProblems()) {
                    out.add("  - " + problem);
                }
            }
            out.add("");
            out.add("| Date | Bucket | Hours | Rate | Amount | Why |");
            out.add("|---|---|---:|---:|---:|---|");
            for (EarningsLine line : person.result().lines()) {
                out.add("| " + line.workDate()
                        + " | " + line.bucket()
                        + " | " + line.hours()
                        + " | $" + line.rate()
                        + " | $" + line.amount()
                        + " | " + line.explanation().narrative()
                        + " (" + line.explanation().citation() + ") |");
            }
            if (person.result().lines().isEmpty()) {
                out.add("| — | — | — | — | — | No earnings lines (exempt or no punches). |");
            }
            out.add("");
        }
        out.add("## Next step");
        out.add("");
        out.add("If this report is useful, run the **next** pay period as a $150 done-with-you audit.");
        out.add("If they pay $150, they will pay $299/month. If they will not, do not start Step 2.");
        out.add("");
        return out.toString();
    }
}
