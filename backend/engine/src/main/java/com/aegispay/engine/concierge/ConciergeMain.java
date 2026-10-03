package com.aegispay.engine.concierge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Step 1.4 CLI. Example:
 *
 * <pre>
 *   java -cp engine/build/libs/engine.jar com.aegispay.engine.concierge.ConciergeMain \
 *     step-01-validation/samples/employees.csv \
 *     step-01-validation/samples/punches.csv \
 *     step-01-validation/samples/attestations.csv \
 *     step-01-validation/samples/bonuses.csv \
 *     step-01-validation/out/autopsy.md
 * </pre>
 */
public final class ConciergeMain {

    private ConciergeMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ConciergeMain <employees.csv> <punches.csv> [attestations.csv] [bonuses.csv] [out.md]");
            System.err.println("CSV columns:");
            System.err.println("  employees: employee_code,label,hourly_rate,exemption,jurisdictions,timezone,job_code");
            System.err.println("  punches:   employee_code,timestamp,type,location");
            System.err.println("  attest:    employee_code,work_date,meal,rest   (YES|NO|WAIVED|UNKNOWN)");
            System.err.println("  bonuses:   employee_code,amount,earned_on,discretionary,note");
            System.exit(2);
        }
        Path employees = Path.of(args[0]);
        Path punches = Path.of(args[1]);
        Path attestations = args.length > 2 && !args[2].isBlank() ? Path.of(args[2]) : null;
        Path bonuses = args.length > 3 && !args[3].isBlank() ? Path.of(args[3]) : null;
        Path out = args.length > 4 ? Path.of(args[4]) : null;

        ConciergeAudit.AutopsyReport report = new ConciergeAudit().run(employees, punches, attestations, bonuses);
        String markdown = AutopsyRenderer.markdown(report);
        if (out != null) {
            Files.createDirectories(out.getParent() == null ? Path.of(".") : out.getParent());
            Files.writeString(out, markdown, StandardCharsets.UTF_8);
            System.out.println("Wrote " + out.toAbsolutePath());
        } else {
            System.out.print(markdown);
        }
        System.exit(report.foundMispayment() ? 0 : 1);
    }
}
