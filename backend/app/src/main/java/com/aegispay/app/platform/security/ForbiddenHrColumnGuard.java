package com.aegispay.app.platform.security;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * v1 does not store SSN, bank accounts, or driver's licenses. Reject the column.
 */
public final class ForbiddenHrColumnGuard {

    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern HEADER = Pattern.compile("ssn|social.?security|bank.?account|routing|driver.?licen", Pattern.CASE_INSENSITIVE);

    private ForbiddenHrColumnGuard() {
    }

    public static void assertSafeCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        String[] lines = csv.split("\\R", 2);
        String header = lines[0].toLowerCase(Locale.ROOT);
        if (HEADER.matcher(header).find()) {
            throw new IllegalArgumentException("CSV contains an SSN or bank column. Strip it and re-upload. AegisPay does not store that data.");
        }
        if (SSN.matcher(csv).find()) {
            throw new IllegalArgumentException("CSV appears to contain Social Security numbers. Strip that column and re-upload.");
        }
    }
}
