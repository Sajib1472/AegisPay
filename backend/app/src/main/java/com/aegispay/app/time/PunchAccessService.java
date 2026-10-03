package com.aegispay.app.time;

import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PunchAccessService {

    private final PunchRepository punches;

    public PunchAccessService(PunchRepository punches) {
        this.punches = punches;
    }

    public Punch require(UUID punchId) {
        return punches.findByTenantIdAndId(TenantContext.requireTenantId(), punchId)
                .orElseThrow(() -> new NotFoundException("Punch not found"));
    }
}
