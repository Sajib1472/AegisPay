package com.aegispay.app.audit;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditPdfRendererTest {

    @Test
    void regeneratingTheSameLinesProducesTheSameChecksum() throws Exception {
        List<String> lines = List.of(
                "AEGISPAY AUDIT PACK",
                "Tenant: Harbor Dental",
                "Period: 2024-06-03 to 2024-06-09",
                "Engine: 0.2.0",
                AuditPackService.DISCLAIMER
        );
        byte[] a = AuditPdfRenderer.render(lines);
        byte[] b = AuditPdfRenderer.render(lines);
        assertArrayEquals(a, b);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(a));
        assertEquals(digest, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)));
        assertTrue(new String(a, StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
    }
}
