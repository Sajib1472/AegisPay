package com.aegispay.app.payroll;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shadow mode: CSV diff, not a machine-learning toy.
 * “Your Excel is $312 lower because CA daily OT on Thursday was not applied to Maria.”
 */
@Service
public class ShadowComparisonService {

    public List<DiffRow> diff(String theirs, String ours) {
        Map<String, BigDecimal> a = index(theirs);
        Map<String, BigDecimal> b = index(ours);
        List<DiffRow> rows = new ArrayList<>();
        for (String key : b.keySet()) {
            BigDecimal left = a.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal right = b.get(key);
            if (left.compareTo(right) != 0) {
                rows.add(new DiffRow(key, left, right, right.subtract(left)));
            }
        }
        for (String key : a.keySet()) {
            if (!b.containsKey(key)) {
                rows.add(new DiffRow(key, a.get(key), BigDecimal.ZERO, a.get(key).negate()));
            }
        }
        return rows;
    }

    private static Map<String, BigDecimal> index(String csv) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) {
            return map;
        }
        String[] lines = csv.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cols = lines[i].split(",", -1);
            String code = cols.length > 0 ? cols[0].replace("\"", "") : "";
            String type = cols.length > 1 ? cols[1].replace("\"", "") : "";
            String amount = cols.length > 3 ? cols[3].replace("\"", "") : (cols.length > 2 ? cols[2].replace("\"", "") : "0");
            BigDecimal value;
            try {
                value = new BigDecimal(amount.isBlank() ? "0" : amount);
            } catch (NumberFormatException e) {
                value = BigDecimal.ZERO;
            }
            map.merge(code + "|" + type, value, BigDecimal::add);
        }
        return map;
    }

    public record DiffRow(String key, BigDecimal theirs, BigDecimal ours, BigDecimal delta) {
    }
}
