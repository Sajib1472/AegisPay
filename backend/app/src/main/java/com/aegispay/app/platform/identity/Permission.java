package com.aegispay.app.platform.identity;

import java.util.List;

public enum Permission {
    MANAGE_BILLING,
    MANAGE_ORG,
    IMPORT_TIME,
    VIEW_REGISTER,
    PAYROLL_APPROVE,
    VIEW_RISK_DASHBOARD;

    public static List<Permission> forRole(UserRole role) {
        return switch (role) {
            case OWNER, PLATFORM_ADMIN -> List.of(values());
            case PAYROLL_ADMIN -> List.of(MANAGE_ORG, IMPORT_TIME, VIEW_REGISTER, PAYROLL_APPROVE, VIEW_RISK_DASHBOARD);
            case LOCATION_MANAGER -> List.of(IMPORT_TIME, VIEW_REGISTER);
            case VIEWER -> List.of(VIEW_REGISTER);
            case STAFF -> List.of();
        };
    }
}
