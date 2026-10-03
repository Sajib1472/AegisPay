package com.aegispay.engine.time;

import com.aegispay.engine.model.WorkPeriod.IntervalType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PunchPairerTest {

    private final PunchPairer pairer = new PunchPairer();

    @Test
    void missingOutIsAProblem() {
        var result = pairer.pair(List.of(raw("2024-06-03T08:00:00Z", PunchPairer.PunchKind.IN)));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("Missing OUT")));
    }

    @Test
    void doubleInClosesPreviousShift() {
        var result = pairer.pair(List.of(
                raw("2024-06-03T08:00:00Z", PunchPairer.PunchKind.IN),
                raw("2024-06-03T12:00:00Z", PunchPairer.PunchKind.IN),
                raw("2024-06-03T17:00:00Z", PunchPairer.PunchKind.OUT)
        ));
        assertEquals(2, result.intervals().size());
    }

    @Test
    void breakStartWithoutEndIsAProblem() {
        var result = pairer.pair(List.of(
                raw("2024-06-03T08:00:00Z", PunchPairer.PunchKind.IN),
                raw("2024-06-03T12:00:00Z", PunchPairer.PunchKind.BREAK_START)
        ));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("Missing BREAK_END")));
    }

    @Test
    void crossMidnightUrgentCareShiftPairs() {
        var result = pairer.pair(List.of(
                raw("2024-06-03T19:00:00Z", PunchPairer.PunchKind.IN),
                raw("2024-06-04T07:00:00Z", PunchPairer.PunchKind.OUT)
        ));
        assertEquals(1, result.intervals().size());
        assertEquals(IntervalType.WORK, result.intervals().get(0).type());
    }

    @Test
    void transferClosesLocationAAndOpensB() {
        var result = pairer.pair(List.of(
                new PunchPairer.RawPunch(Instant.parse("2024-06-03T08:00:00Z"), PunchPairer.PunchKind.IN, "A", "RDH"),
                new PunchPairer.RawPunch(Instant.parse("2024-06-03T12:00:00Z"), PunchPairer.PunchKind.TRANSFER, "B", "RDH"),
                new PunchPairer.RawPunch(Instant.parse("2024-06-03T16:00:00Z"), PunchPairer.PunchKind.OUT, "B", "RDH")
        ));
        assertEquals(2, result.intervals().stream().filter(i -> i.type() == IntervalType.WORK).count());
    }

    private static PunchPairer.RawPunch raw(String at, PunchPairer.PunchKind kind) {
        return new PunchPairer.RawPunch(Instant.parse(at), kind, "loc", "RDH");
    }
}
