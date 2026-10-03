package com.aegispay.app.rules;

import com.aegispay.engine.rules.PublishedRulePack;
import com.aegispay.engine.rules.RulePack;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payroll uses the packs that were effective then, unless the caller asks for current law.
 * Tenants cannot publish packs; platform admin does.
 */
@Service
public class RulePackResolver {

    public enum LawMode {
        HISTORICAL,
        CURRENT_LAW
    }

    private final RulePackRepository packs;

    public RulePackResolver(RulePackRepository packs) {
        this.packs = packs;
    }

    public Resolved resolve(LocalDate periodEnd, LawMode mode) {
        LocalDate asOf = mode == LawMode.CURRENT_LAW ? LocalDate.now() : periodEnd;
        List<RulePackRecord> published = packs.findByStatusAndEffectiveFromLessThanEqual("PUBLISHED", asOf);
        List<RulePackRecord> active = published.stream()
                .filter(p -> p.getEffectiveTo() == null || !p.getEffectiveTo().isBefore(asOf))
                .toList();
        List<RulePack> engine = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (RulePackRecord record : active) {
            engine.add(new PublishedRulePack(record.getJurisdiction(), record.getVersion(), record.getEngineModule()));
            ids.add(record.getId());
            labels.add(record.getJurisdiction() + "@" + record.getVersion());
        }
        if (engine.isEmpty()) {
            engine.add(PublishedRulePack.flsa());
            engine.add(PublishedRulePack.california());
            labels.add("US-FLSA@2024.1");
            labels.add("US-CA@2024.1");
        }
        return new Resolved(engine, ids, labels, mode);
    }

    public record Resolved(List<RulePack> engine, List<UUID> ids, List<String> labels, LawMode mode) {
    }
}
