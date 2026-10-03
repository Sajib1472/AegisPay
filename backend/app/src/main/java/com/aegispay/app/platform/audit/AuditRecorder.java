package com.aegispay.app.platform.audit;

import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRecorder {

    private final AuditEventRepository events;

    public AuditRecorder(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(String action, String entityType, String entityId) {
        AuditEvent event = new AuditEvent();
        event.setTenantId(TenantContext.tenantId());
        event.setActorId(TenantContext.userId());
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setRequestId(TenantContext.requestId());
        events.save(event);
    }
}
