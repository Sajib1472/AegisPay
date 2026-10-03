package com.aegispay.app.platform.ops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaffoldConventionsTest {

    @Test
    void apiAndHealthConventions() {
        assertEquals("/api/v1", ScaffoldConventions.API_PREFIX);
        assertEquals("application/problem+json", ScaffoldConventions.PROBLEM_JSON);
        assertEquals("Idempotency-Key", ScaffoldConventions.IDEMPOTENCY_HEADER);
        assertEquals("validate", ScaffoldConventions.DDL_AUTO);
        assertEquals(50, ScaffoldConventions.PUNCH_PAGE_SIZE);
    }
}
