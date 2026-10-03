package com.aegispay.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForeverFixtureCatalogTest {

    @Test
    void customerBugFixturesStayInTheRepo() {
        for (String name : List.of(
                "fixtures/ca_missed_meal.csv",
                "fixtures/ca_nine_hour_day.csv",
                "fixtures/flsa_bonus_trueup.csv",
                "fixtures/dual_rate_hygienist.csv",
                "fixtures/cross_midnight_urgent_care.csv",
                "fixtures/gusto-import.csv"
        )) {
            assertNotNull(getClass().getResource("/" + name), name);
            assertTrue(name.endsWith(".csv"));
        }
    }
}
