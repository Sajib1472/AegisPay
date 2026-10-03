package com.aegispay.app.billing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class StripeWebhookController {

    private final StripeBillingService stripe;

    public StripeWebhookController(StripeBillingService stripe) {
        this.stripe = stripe;
    }

    @PostMapping("/webhooks/stripe")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> stripe(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        return Map.of("status", stripe.handleWebhook(signature, payload));
    }
}
