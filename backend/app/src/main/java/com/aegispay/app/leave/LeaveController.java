package com.aegispay.app.leave;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave")
public class LeaveController {

    private final LeaveAccrualService leave;

    public LeaveController(LeaveAccrualService leave) {
        this.leave = leave;
    }

    @GetMapping("/balances")
    public List<LeaveBalance> balances() {
        return leave.list();
    }

    @PostMapping("/usage")
    @PreAuthorize("hasAnyAuthority('PAYROLL_APPROVE','MANAGE_ORG')")
    public LeaveLedgerEntry usage(@RequestBody UsageBody body) {
        return leave.recordUsage(body.personId(), body.hours(), body.workDate(), body.note());
    }

    public record UsageBody(UUID personId, BigDecimal hours, LocalDate workDate, String note) {
    }
}
