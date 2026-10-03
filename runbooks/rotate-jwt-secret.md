# Rotate JWT secret

1. Generate a 64+ character secret.
2. Put it in the host vault as `JWT_SECRET`.
3. Deploy. Existing access tokens die in 15 minutes; refresh tokens are hashed in Postgres and still work until expiry unless you truncate `refresh_token`.
4. To force everyone out: `TRUNCATE refresh_token;`
5. Confirm login still works. Never log the new secret.
