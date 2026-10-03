package com.aegispay.app.platform.tenancy;

import com.aegispay.app.time.Punch;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantEntityGuardTest {

    @Test
    void businessRowsCannotSkipTenantId() throws Exception {
        TenantContext.clear();
        Punch punch = new Punch();
        Method prePersist = punch.getClass().getSuperclass().getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> prePersist.invoke(punch));
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }
}
