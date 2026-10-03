package com.aegispay.engine.time;

import com.aegispay.engine.model.WorkPeriod.IntervalType;
import com.aegispay.engine.time.PunchPairer.PunchKind;
import com.aegispay.engine.time.PunchPairer.RawPunch;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunchPairerTest {

    private final PunchPairer pairer = new PunchPairer();

    @Test
    void pairsInBreakOut() {
        Instant t0 = Instant.parse("2024-06-03T15:00:00Z");
        Instant t1 = Instant.parse("2024-06-03T20:00:00Z");
        Instant t2 = Instant.parse("2024-06-03T20:30:00Z");
        Instant t3 = Instant.parse("2024-06-03T23:00:00Z");
        var result = pairer.pair(List.of(
                new RawPunch(t0, PunchKind.IN, "loc", "RDH"),
                new RawPunch(t1, PunchKind.BREAK_START, "loc", "RDH"),
                new RawPunch(t2, PunchKind.BREAK_END, "loc", "RDH"),
                new RawPunch(t3, PunchKind.OUT, "loc", "RDH")
        ));
        assertEquals(3, result.intervals().size());
        assertEquals(IntervalType.UNPAID_MEAL, result.intervals().get(1).type());
        assertTrue(result.problems().isEmpty());
    }

    @Test
    void missingOutIsAProblem() {
        Instant t0 = Instant.parse("2024-06-03T15:00:00Z");
        var result = pairer.pair(List.of(new RawPunch(t0, PunchKind.IN, "loc", "RDH")));
        assertEquals(1, result.problems().size());
        assertTrue(result.intervals().isEmpty());
    }
}
