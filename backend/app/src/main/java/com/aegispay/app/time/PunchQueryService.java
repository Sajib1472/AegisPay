package com.aegispay.app.time;

import com.aegispay.app.platform.identity.UserRole;
import com.aegispay.app.platform.ops.ScaffoldConventions;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.aegispay.app.web.PageResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PunchQueryService {

    private final PunchRepository punches;

    public PunchQueryService(PunchRepository punches) {
        this.punches = punches;
    }

    public PageResponse<Punch> page(String cursorRaw, Integer limit) {
        int size = limit == null || limit <= 0 ? ScaffoldConventions.PUNCH_PAGE_SIZE : Math.min(limit, 200);
        UUID tenantId = TenantContext.requireTenantId();
        PunchCursor cursor = PunchCursor.parse(cursorRaw);
        List<Punch> rows = punches.findByTenantIdOrderByAdjustedAtDesc(tenantId);
        UUID locationScope = TenantContext.locationId();
        if (locationScope != null && UserRole.LOCATION_MANAGER.name().equals(TenantContext.role())) {
            rows = rows.stream().filter(p -> locationScope.equals(p.getLocationId())).toList();
        }
        List<Punch> sliced = rows.stream()
                .filter(p -> cursor == null || p.getAdjustedAt().isBefore(cursor.adjustedAt())
                        || (p.getAdjustedAt().equals(cursor.adjustedAt()) && p.getId().toString().compareTo(cursor.id().toString()) < 0))
                .limit(size + 1L)
                .toList();
        boolean more = sliced.size() > size;
        List<Punch> items = more ? sliced.subList(0, size) : sliced;
        String next = null;
        if (more && !items.isEmpty()) {
            Punch last = items.get(items.size() - 1);
            next = new PunchCursor(last.getAdjustedAt(), last.getId()).encode();
        }
        return new PageResponse<>(TenantContext.requestId(), items, next, size);
    }
}
