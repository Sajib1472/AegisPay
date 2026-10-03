package com.aegispay.engine;

import com.aegispay.engine.rules.RulePack;

import java.util.List;

public record EngineOptions(
        boolean includeCalifornia,
        boolean enableFluctuatingWorkweek
) {
    public static EngineOptions fromPacks(List<RulePack> packs) {
        boolean ca = packs.stream().anyMatch(p -> "US-CA".equals(p.jurisdiction()) || p.jurisdiction().startsWith("US-CA-"));
        return new EngineOptions(ca, false);
    }
}
