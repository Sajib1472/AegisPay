# Data processing addendum (outline)

Customer is the controller of employment data. AegisPay processes punches and pay rates solely to produce earnings, exceptions, and exports.

Subprocessors (typical): hosting, Postgres, Redis, object storage, email (Postmark/SES), Stripe for cards.

On cancel: 30 days read-only, then hard delete per policy. Customer may export punches and runs at any time.
