package com.aegispay.app.org;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Most-specific last so city can override state can override FLSA.
 */
public final class JurisdictionResolver {

    private static final Set<String> CA_CITIES_WITH_LOCAL = Set.of("santa monica", "los angeles", "san francisco", "oakland", "berkeley");

    private JurisdictionResolver() {
    }

    public static List<String> resolve(String city, String region, String country) {
        List<String> codes = new ArrayList<>();
        codes.add("US-FLSA");
        String state = region == null ? "" : region.trim().toUpperCase(Locale.ROOT);
        if ("CA".equals(state) || "CALIFORNIA".equals(state)) {
            codes.add("US-CA");
            String c = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
            if (CA_CITIES_WITH_LOCAL.contains(c) && "santa monica".equals(c)) {
                codes.add("US-CA-SANTA-MONICA");
            }
        } else if ("TX".equals(state) || "TEXAS".equals(state)) {
            codes.add("US-TX");
        } else if ("NY".equals(state) || "NEW YORK".equals(state)) {
            codes.add("US-NY");
        } else if (!state.isBlank()) {
            codes.add("US-" + state);
        }
        return List.copyOf(codes);
    }

    public static String inferTimeZone(String region) {
        return Map.of(
                "CA", "America/Los_Angeles",
                "TX", "America/Chicago",
                "NY", "America/New_York",
                "WA", "America/Los_Angeles",
                "CO", "America/Denver",
                "IL", "America/Chicago"
        ).getOrDefault(region == null ? "" : region.toUpperCase(Locale.ROOT), "America/Los_Angeles");
    }
}
