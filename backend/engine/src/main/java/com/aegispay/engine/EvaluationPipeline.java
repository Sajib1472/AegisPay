package com.aegispay.engine;

/**
 * Fixed evaluation order. If you change it, bump {@link EngineVersion}.
 */
public enum EvaluationPipeline {
    NORMALIZE_INTERVALS,
    CLASSIFY,
    APPLY_ROUNDING,
    DETECT_MEAL_REST,
    DAILY_BUCKETS,
    WEEKLY_FLSA,
    DIFFERENTIALS,
    REGULAR_RATE,
    ALLOCATE_PRODUCTION_BONUS,
    PREMIUM_PAY,
    LEAVE_ACCRUAL_LATER,
    EMIT_EXPLANATIONS
}
