package com.aegispay.app.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeSignatureTest {

    @Test
    void validV1SignatureIsAccepted() {
        String payload = "{\"id\":\"evt_test\",\"type\":\"invoice.paid\"}";
        String secret = "whsec_local";
        long t = 1_700_000_000L;
        String header = "t=" + t + ",v1=" + StripeSignature.hmac(secret, t + "." + payload);
        assertDoesNotThrow(() -> StripeSignature.verify(header, payload, secret, t, 300));
    }

    @Test
    void wrongSecretIsRejected() {
        String payload = "{\"id\":\"evt_test\",\"type\":\"invoice.paid\"}";
        long t = 1_700_000_000L;
        String header = "t=" + t + ",v1=" + StripeSignature.hmac("whsec_local", t + "." + payload);
        assertThrows(IllegalArgumentException.class,
                () -> StripeSignature.verify(header, payload, "whsec_other", t, 300));
    }
}
