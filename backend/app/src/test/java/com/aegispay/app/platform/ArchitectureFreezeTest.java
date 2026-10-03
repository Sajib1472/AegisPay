package com.aegispay.app.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureFreezeTest {

    @Test
    void frozenChoices() {
        assertEquals("SHARED_SCHEMA_DISCRIMINATOR", ArchitectureFreeze.TENANCY);
        assertEquals("MODULAR_MONOLITH", ArchitectureFreeze.MONOLITH);
        assertEquals("PURE_JAVA_NO_SPRING", ArchitectureFreeze.ENGINE);
        assertEquals("NUMERIC(12,4)", ArchitectureFreeze.MONEY_SQL);
        assertEquals("NUMERIC(8,4)", ArchitectureFreeze.HOURS_SQL);
        assertEquals("HANDWRITTEN", ArchitectureFreeze.MAPPERS);
        assertEquals("21", ArchitectureFreeze.JAVA);
        assertEquals("3.3", ArchitectureFreeze.SPRING_BOOT);
        assertEquals("16", ArchitectureFreeze.POSTGRES);
        assertEquals("7", ArchitectureFreeze.REDIS);
        assertEquals(1, ArchitectureFreeze.DEFAULT_PUNCH_ROUND_MINUTES);
        assertEquals("platform", ArchitectureFreeze.PLATFORM_SCHEMA);
    }
}
