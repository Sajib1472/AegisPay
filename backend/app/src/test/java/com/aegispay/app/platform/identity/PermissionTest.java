package com.aegispay.app.platform.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionTest {

    @Test
    void onlyOwnerAndPayrollAdminCanApprove() {
        assertTrue(Permission.forRole(UserRole.OWNER).contains(Permission.PAYROLL_APPROVE));
        assertTrue(Permission.forRole(UserRole.PAYROLL_ADMIN).contains(Permission.PAYROLL_APPROVE));
        assertFalse(Permission.forRole(UserRole.VIEWER).contains(Permission.PAYROLL_APPROVE));
        assertFalse(Permission.forRole(UserRole.STAFF).contains(Permission.PAYROLL_APPROVE));
        assertTrue(Permission.forRole(UserRole.VIEWER).contains(Permission.VIEW_REGISTER));
    }
}
