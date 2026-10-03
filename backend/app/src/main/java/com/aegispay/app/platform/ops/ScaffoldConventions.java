package com.aegispay.app.platform.ops;

/**
 * Step 5 API and ops freeze.
 */
public final class ScaffoldConventions {

    public static final String API_PREFIX = "/api/v1";
    public static final String PROBLEM_JSON = "application/problem+json";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final int PUNCH_PAGE_SIZE = 50;
    public static final String DDL_AUTO = "validate";

    private ScaffoldConventions() {
    }
}
