package com.aegispay.app.billing;

import com.aegispay.app.platform.identity.Tenant;
import com.aegispay.app.platform.identity.TenantRepository;
import com.aegispay.app.platform.tenancy.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class StripeBillingService {

    public static final Duration GRACE = Duration.ofDays(7);

    private final SubscriptionRepository subscriptions;
    private final TenantRepository tenants;
    private final StripeEventRepository events;
    private final TenantEntitlementRepository extras;
    private final ObjectMapper json;
    private final String webhookSecret;
    private final String secretKey;
    private final String frontendOrigin;
    private final String priceGroupMonthly;
    private final String priceGroupAnnual;
    private final String priceNetworkMonthly;
    private final String priceNetworkAnnual;

    public StripeBillingService(
            SubscriptionRepository subscriptions,
            TenantRepository tenants,
            StripeEventRepository events,
            TenantEntitlementRepository extras,
            ObjectMapper json,
            @Value("${aegispay.stripe.webhook-secret:whsec_local}") String webhookSecret,
            @Value("${aegispay.stripe.secret-key:}") String secretKey,
            @Value("${aegispay.cors.origin}") String frontendOrigin,
            @Value("${aegispay.stripe.price-group-monthly:price_group_monthly}") String priceGroupMonthly,
            @Value("${aegispay.stripe.price-group-annual:price_group_annual}") String priceGroupAnnual,
            @Value("${aegispay.stripe.price-network-monthly:price_network_monthly}") String priceNetworkMonthly,
            @Value("${aegispay.stripe.price-network-annual:price_network_annual}") String priceNetworkAnnual
    ) {
        this.subscriptions = subscriptions;
        this.tenants = tenants;
        this.events = events;
        this.extras = extras;
        this.json = json;
        this.webhookSecret = webhookSecret;
        this.secretKey = secretKey;
        this.frontendOrigin = frontendOrigin;
        this.priceGroupMonthly = priceGroupMonthly;
        this.priceGroupAnnual = priceGroupAnnual;
        this.priceNetworkMonthly = priceNetworkMonthly;
        this.priceNetworkAnnual = priceNetworkAnnual;
    }

    public Map<String, String> checkout(String plan, boolean annual) {
        UUID tenantId = TenantContext.requireTenantId();
        String price = priceId(plan, annual);
        if (secretKey == null || secretKey.isBlank()) {
            return Map.of(
                    "url", frontendOrigin + "/settings?checkout=simulated&plan=" + plan + "&annual=" + annual,
                    "mode", "local",
                    "priceId", price,
                    "tenantId", tenantId.toString()
            );
        }
        return Map.of(
                "url", "https://checkout.stripe.com/c/pay/simulated_" + price,
                "mode", "stripe",
                "priceId", price,
                "tenantId", tenantId.toString()
        );
    }

    public Map<String, String> portal() {
        UUID tenantId = TenantContext.requireTenantId();
        Subscription sub = subscriptions.findByTenantId(tenantId).orElseThrow();
        if (secretKey == null || secretKey.isBlank()) {
            return Map.of("url", frontendOrigin + "/settings?portal=simulated", "customerId",
                    sub.getStripeCustomerId() == null ? "" : sub.getStripeCustomerId());
        }
        return Map.of("url", "https://billing.stripe.com/p/session/simulated",
                "customerId", sub.getStripeCustomerId() == null ? "" : sub.getStripeCustomerId());
    }

    @Transactional
    public String handleWebhook(String signatureHeader, String payload) {
        StripeSignature.verify(signatureHeader, payload, webhookSecret, Instant.now().getEpochSecond(), 300);
        JsonNode root;
        try {
            root = json.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe payload");
        }
        String id = root.path("id").asText();
        String type = root.path("type").asText();
        if (id.isBlank()) {
            throw new IllegalArgumentException("Stripe event missing id");
        }
        if (events.existsById(id)) {
            return "duplicate";
        }
        JsonNode object = root.path("data").path("object");
        switch (type) {
            case "checkout.session.completed" -> checkoutCompleted(object);
            case "invoice.paid" -> invoicePaid(object);
            case "invoice.payment_failed" -> invoiceFailed(object);
            case "customer.subscription.updated", "customer.subscription.deleted" -> subscriptionChanged(object, type);
            default -> {
            }
        }
        StripeEvent row = new StripeEvent();
        row.setId(id);
        row.setType(type);
        events.save(row);
        return type;
    }

    public void enforceGrace(Tenant tenant) {
        subscriptions.findByTenantId(tenant.getId()).ifPresent(sub -> {
            if ("PAST_DUE".equals(sub.getStatus())
                    && sub.getGraceUntil() != null
                    && Instant.now().isAfter(sub.getGraceUntil())) {
                tenant.setStatus("READ_ONLY");
                tenants.save(tenant);
            }
        });
    }

    private void checkoutCompleted(JsonNode session) {
        String tenantRaw = session.path("client_reference_id").asText(null);
        if (tenantRaw == null || tenantRaw.isBlank()) {
            tenantRaw = session.path("metadata").path("tenantId").asText(null);
        }
        if (tenantRaw == null) {
            return;
        }
        UUID tenantId = UUID.fromString(tenantRaw);
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        Subscription sub = subscriptions.findByTenantId(tenantId).orElseGet(Subscription::new);
        sub.setTenantId(tenantId);
        sub.setStripeCustomerId(text(session, "customer"));
        sub.setStripeSubscriptionId(text(session, "subscription"));
        String plan = session.path("metadata").path("plan").asText("GROUP");
        boolean annual = session.path("metadata").path("annual").asBoolean(false);
        sub.setPlan(plan);
        sub.setAnnual(annual);
        sub.setStatus("ACTIVE");
        sub.setGraceUntil(null);
        subscriptions.save(sub);
        tenant.setPlan(plan);
        tenant.setStatus("ACTIVE");
        tenants.save(tenant);
        if (session.path("metadata").path("auditPack").asBoolean(false)) {
            enableExtra(tenantId, "AUDIT_PACK");
        }
    }

    private void invoicePaid(JsonNode invoice) {
        findByCustomer(text(invoice, "customer")).ifPresent(sub -> {
            sub.setStatus("ACTIVE");
            sub.setGraceUntil(null);
            sub.setCurrentPeriodEnd(Instant.now().plus(Duration.ofDays(sub.isAnnual() ? 365 : 30)));
            subscriptions.save(sub);
            tenants.findById(sub.getTenantId()).ifPresent(t -> {
                t.setStatus("ACTIVE");
                tenants.save(t);
            });
        });
    }

    private void invoiceFailed(JsonNode invoice) {
        findByCustomer(text(invoice, "customer")).ifPresent(sub -> {
            sub.setStatus("PAST_DUE");
            sub.setGraceUntil(Instant.now().plus(GRACE));
            subscriptions.save(sub);
        });
    }

    private void subscriptionChanged(JsonNode subscription, String type) {
        findByCustomer(text(subscription, "customer")).ifPresent(sub -> {
            if ("customer.subscription.deleted".equals(type)) {
                sub.setStatus("CANCELED");
                subscriptions.save(sub);
                tenants.findById(sub.getTenantId()).ifPresent(t -> {
                    t.setStatus("READ_ONLY");
                    tenants.save(t);
                });
                return;
            }
            String status = subscription.path("status").asText("active").toUpperCase();
            sub.setStatus("ACTIVE".equals(status) || "TRIALING".equals(status) ? status : status);
            subscriptions.save(sub);
        });
    }

    private void enableExtra(UUID tenantId, String code) {
        TenantContext.set(tenantId, null, "OWNER", "stripe");
        try {
            boolean exists = extras.findByTenantId(tenantId).stream().anyMatch(e -> code.equals(e.getCode()));
            if (!exists) {
                TenantEntitlement row = new TenantEntitlement();
                row.setTenantId(tenantId);
                row.setCode(code);
                row.setEnabled(true);
                extras.save(row);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private java.util.Optional<Subscription> findByCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return java.util.Optional.empty();
        }
        return subscriptions.findByStripeCustomerId(customerId);
    }

    private String priceId(String plan, boolean annual) {
        String code = plan == null ? "GROUP" : plan.toUpperCase();
        if ("NETWORK".equals(code)) {
            return annual ? priceNetworkAnnual : priceNetworkMonthly;
        }
        return annual ? priceGroupAnnual : priceGroupMonthly;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isObject()) {
            return value.path("id").asText(null);
        }
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }
}
