package com.aegispay.app.rules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RulePackDocumentRepository extends JpaRepository<RulePackDocument, UUID> {
    List<RulePackDocument> findByRulePackId(UUID rulePackId);
}
