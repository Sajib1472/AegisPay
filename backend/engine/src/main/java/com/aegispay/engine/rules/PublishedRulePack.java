package com.aegispay.engine.rules;

public record PublishedRulePack(
        String jurisdiction,
        String version,
        String engineModule
) implements RulePack {
    public static PublishedRulePack flsa() {
        return new PublishedRulePack("US-FLSA", "2024.1", "FLSA");
    }

    public static PublishedRulePack california() {
        return new PublishedRulePack("US-CA", "2024.1", "CALIFORNIA");
    }
}
