package com.aegispay.app.shadow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pilot shadow mode: CSV diff against the clinic's Excel/Gusto file. Not ML.
 */
public final class ShadowCsvDiff {

    private ShadowCsvDiff() {
    }

    public static List<Row> compare(String theirs, String ours) {
        Map<String, BigDecimal> a = index(theirs);
        Map<String, BigDecimal> b = index(ours);
        List<Row> rows = new ArrayList<>();
        a.forEach((key, amount) -> {
            BigDecimal other = b.getOrDefault(key, BigDecimal.ZERO);
            if (amount.compareTo(other) != 0) {
                rows.add(new Row(key, amount.toPlainString(), other.toPlainString(),
                        "Your file is " + amount.subtract(other).abs().toPlainString()
                                + (amount.compareTo(other) > 0 ? " higher" : " lower") + " on " + key));
            }
        });
        b.forEach((key, amount) -> {
            if (!a.containsKey(key)) {
                rows.add(new Row(key, "0", amount.toPlainString(), "AegisPay booked this line; your Excel did not"));
            }
        });
        return rows;
    }

    private static Map<String, BigDecimal> index(String csv) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) {
            return map;
        }
        String[] lines = csv.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && lines[i].toLowerCase().contains("employee")) {
                continue;
            }
            String[] cols = lines[i].split(",", -1);
            if (cols.length < 4) {
                continue;
            }
            String key = cols[0].trim() + "|" + cols[1].trim();
            try {
                map.merge(key, new BigDecimal(cols[3].trim()), BigDecimal::add);
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    public record Row(String key, String theirs, String ours, String note) {
    }
}
