package com.aegispay.engine.leave;

import com.aegispay.engine.money.Hours;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaveAccrualCalculatorTest {

    @Test
    void thirtyHoursWorkedEarnsOneSickHour() {
        Hours earned = LeaveAccrualCalculator.accrue(Hours.of("30"), Hours.ZERO, LeaveAccrualCalculator.Policy.caStatewide());
        assertEquals(Hours.of("1"), earned);
    }

    @Test
    void capStopsAccrual() {
        Hours earned = LeaveAccrualCalculator.accrue(Hours.of("90"), Hours.of("39.5000"), LeaveAccrualCalculator.Policy.caStatewide());
        assertEquals(Hours.of("0.5000"), earned);
    }

    @Test
    void cityOverlayIsMoreGenerousThanStatewide() {
        assertTrue(LeaveAccrualCalculator.resolve(List.of("US-FLSA", "US-CA", "US-CA-SANTA-MONICA"))
                .capHours()
                .isGreaterThan(LeaveAccrualCalculator.Policy.caStatewide().capHours()));
    }
}
