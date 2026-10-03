package com.aegispay.engine.export;

import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningsLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps engine buckets to a Gusto-style additional-earnings CSV.
 */
public final class GustoCsvExporter {

    public String export(String employeeCode, EarningsResult result) {
        List<String> rows = new ArrayList<>();
        rows.add("employee_code,earning_type,hours,amount,note");
        for (EarningsLine line : result.lines()) {
            rows.add(String.join(",",
                    csv(employeeCode),
                    csv(gustoType(line)),
                    csv(line.hours().toString()),
                    csv(line.amount().toStatement().toPlainString()),
                    csv(line.explanation().narrative())
            ));
        }
        return String.join("\n", rows) + "\n";
    }

    private static String gustoType(EarningsLine line) {
        return switch (line.bucket()) {
            case REG -> "Regular Hours";
            case OT_1_5 -> "Overtime";
            case OT_2_0 -> "Double overtime";
            case MEAL_PREMIUM -> "Meal break premium";
            case REST_PREMIUM -> "Rest break premium";
            case SPLIT_SHIFT -> "Split shift premium";
            case REPORTING_TIME -> "Reporting time pay";
            case DIFFERENTIAL -> "Shift differential";
            case BONUS, BONUS_TRUE_UP -> "Bonus";
        };
    }

    private static String csv(String value) {
        String v = value == null ? "" : value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    public static final class Holder {
        private final StringBuilder body = new StringBuilder();
        private boolean headerWritten;

        public void append(String csv) {
            if (csv == null || csv.isBlank()) {
                return;
            }
            int newline = csv.indexOf('\n');
            if (!headerWritten) {
                body.append(csv);
                if (!csv.endsWith("\n")) {
                    body.append('\n');
                }
                headerWritten = true;
            } else if (newline >= 0) {
                body.append(csv.substring(newline + 1));
                if (!csv.endsWith("\n")) {
                    body.append('\n');
                }
            }
        }

        public String merge() {
            return body.toString();
        }
    }

    public String generic(String employeeCode, EarningsResult result) {
        List<String> rows = new ArrayList<>();
        rows.add("employee_code,earning_type,hours,amount,date,location,note");
        for (EarningsLine line : result.lines()) {
            rows.add(String.join(",",
                    csv(employeeCode),
                    csv(line.bucket().name().toLowerCase(Locale.ROOT)),
                    csv(line.hours().toString()),
                    csv(line.amount().toStatement().toPlainString()),
                    csv(line.workDate() == null ? "" : line.workDate().toString()),
                    csv(""),
                    csv(line.explanation().code())
            ));
        }
        return String.join("\n", rows) + "\n";
    }
}
