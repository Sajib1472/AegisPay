package com.aegispay.app.payroll;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Earnings are never deleted. Void with a reversing line.
 */
@Service
public class EarningsLedger {

    private final EarningsLineRepository lines;

    public EarningsLedger(EarningsLineRepository lines) {
        this.lines = lines;
    }

    @Transactional
    public EarningsLineEntity reverse(UUID lineId) {
        EarningsLineEntity original = lines.findById(lineId).orElseThrow();
        EarningsLineEntity reversing = new EarningsLineEntity();
        reversing.setPayRunId(original.getPayRunId());
        reversing.setPayPeriodId(original.getPayPeriodId());
        reversing.setPersonId(original.getPersonId());
        reversing.setWorkDate(original.getWorkDate());
        reversing.setBucket(original.getBucket());
        reversing.setHours(original.getHours() == null ? BigDecimal.ZERO : original.getHours().negate());
        reversing.setRate(original.getRate());
        reversing.setAmount(original.getAmount() == null ? BigDecimal.ZERO : original.getAmount().negate());
        reversing.setExplanation(original.getExplanation());
        reversing.setReversingOf(original.getId());
        return lines.save(reversing);
    }
}
