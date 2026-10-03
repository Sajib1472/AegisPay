package com.aegispay.app.platform;

/**
 * Step 3 freeze. Changing these later is expensive; tests fail if they drift.
 */
public final class ArchitectureFreeze {

    public static final String TENANCY = "SHARED_SCHEMA_DISCRIMINATOR";
    public static final String MONOLITH = "MODULAR_MONOLITH";
    public static final String ENGINE = "PURE_JAVA_NO_SPRING";
    public static final String MONEY_SQL = "NUMERIC(12,4)";
    public static final String HOURS_SQL = "NUMERIC(8,4)";
    public static final String TIMESTAMPS = "TIMESTAMPTZ_UTC";
    public static final String MONEY_ROUNDING = "HALF_UP";
    public static final int MONEY_SCALE = 4;
    public static final int STATEMENT_SCALE = 2;
    public static final int DEFAULT_PUNCH_ROUND_MINUTES = 1;
    public static final String MAPPERS = "HANDWRITTEN";
    public static final String JAVA = "21";
    public static final String SPRING_BOOT = "3.3";
    public static final String POSTGRES = "16";
    public static final String REDIS = "7";
    public static final String PLATFORM_SCHEMA = "platform";

    private ArchitectureFreeze() {
    }
}
