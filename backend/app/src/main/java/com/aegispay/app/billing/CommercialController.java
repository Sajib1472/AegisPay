package com.aegispay.app.billing;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CommercialController {

    private final EntitlementService entitlements;
    private final StripeBillingService stripe;

    public CommercialController(EntitlementService entitlements, StripeBillingService stripe) {
        this.entitlements = entitlements;
        this.stripe = stripe;
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return Map.of(
                "plans", PlanCatalog.entries(),
                "auditPackCents", PlanCode.AUDIT_PACK_CENTS,
                "noFreeForeverPlan", Positioning.NOT_A_FREE_PLAN,
                "positioning", List.of(
                        Positioning.VS_GUSTO,
                        Positioning.VS_HOMEBASE,
                        Positioning.VS_HR_FOR_HEALTH,
                        Positioning.VS_WAGEROOT
                )
        );
    }

    @GetMapping("/account")
    public EntitlementService.AccountView account() {
        return entitlements.current();
    }

    @PostMapping("/billing/checkout")
    @PreAuthorize("hasAuthority('MANAGE_BILLING')")
    public Map<String, String> checkout(@RequestBody CheckoutBody body) {
        return stripe.checkout(body.plan(), body.annual());
    }

    @PostMapping("/billing/portal")
    @PreAuthorize("hasAuthority('MANAGE_BILLING')")
    public Map<String, String> portal() {
        return stripe.portal();
    }

    public record CheckoutBody(String plan, boolean annual) {
    }
}
