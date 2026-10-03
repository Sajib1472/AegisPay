package com.aegispay.app.platform.tenancy;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> LOCATION = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID userId, String role, String requestId) {
        set(tenantId, userId, role, requestId, null);
    }

    public static void set(UUID tenantId, UUID userId, String role, String requestId, UUID locationId) {
        TENANT.set(tenantId);
        USER.set(userId);
        ROLE.set(role);
        REQUEST_ID.set(requestId);
        LOCATION.set(locationId);
    }

    public static UUID tenantId() {
        return TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID id = TENANT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant bound to this request");
        }
        return id;
    }

    public static UUID userId() {
        return USER.get();
    }

    public static String role() {
        return ROLE.get();
    }

    public static String requestId() {
        return REQUEST_ID.get();
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static UUID locationId() {
        return LOCATION.get();
    }

    public static void clear() {
        TENANT.remove();
        USER.remove();
        ROLE.remove();
        REQUEST_ID.remove();
        LOCATION.remove();
    }
}
