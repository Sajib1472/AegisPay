# Stripe webhook replay

1. In Stripe Dashboard (test or live), open the event, click Resend.
2. Staging URL: `https://staging.example/api/v1/billing/webhooks/stripe`
3. Signature must match `STRIPE_WEBHOOK_SECRET`. Wrong secret → 400 (`StripeSignatureTest` fixture).
4. Duplicate `event.id` is stored in `stripe_event` and returns `"duplicate"` — safe to retry.
5. `invoice.payment_failed` starts a 7-day grace; after that `tenant.status = READ_ONLY`.
