package com.aegispay.engine.export;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GustoExportFixtureTest {

    @Test
    void gustoCsvMatchesCheckedInImportFixture() throws Exception {
        LocalDate day = LocalDate.of(2024, 6, 7);
        EarningsResult result = EarningsResult.builder("hygienist", "0.2.0")
                .regularRate(Money.of("30.00"))
                .line(line(day, EarningBucket.REG, "8", "30.00", "240.00", "REG", "Straight time"))
                .line(line(day, EarningBucket.OT_1_5, "2", "45.00", "90.00", "OT_1_5", "Weekly overtime"))
                .line(line(day, EarningBucket.MEAL_PREMIUM, "1", "30.00", "30.00", "MEAL_PREMIUM", "Missed meal premium"))
                .line(line(day, EarningBucket.BONUS, "0", "50.00", "50.00", "BONUS", "Production bonus"))
                .build();
        String expected = new String(
                Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/gusto-import.csv")).readAllBytes(),
                StandardCharsets.UTF_8
        );
        assertEquals(expected, new GustoCsvExporter().export("HD-001", result));
    }

    private static EarningsLine line(
            LocalDate day,
            EarningBucket bucket,
            String hours,
            String rate,
            String amount,
            String code,
            String narrative
    ) {
        return new EarningsLine(
                day,
                bucket,
                Hours.of(hours),
                Money.of(rate),
                Money.of(amount),
                new Explanation(code, "fixture", Map.of(), "", narrative)
        );
    }
}
