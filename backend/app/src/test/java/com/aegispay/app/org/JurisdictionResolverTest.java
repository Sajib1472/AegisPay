package com.aegispay.app.org;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JurisdictionResolverTest {

    @Test
    void santaMonicaIsFlsaThenCaThenCity() {
        List<String> codes = JurisdictionResolver.resolve("Santa Monica", "CA", "US");
        assertEquals(List.of("US-FLSA", "US-CA", "US-CA-SANTA-MONICA"), codes);
    }

    @Test
    void austinStaysFederalPlusTexas() {
        assertEquals(List.of("US-FLSA", "US-TX"), JurisdictionResolver.resolve("Austin", "TX", "US"));
    }
}
