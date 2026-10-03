package com.aegispay.app.rules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RulePackRepository extends JpaRepository<RulePackRecord, UUID> {

    List<RulePackRecord> findByStatusOrderByJurisdictionAscVersionAsc(String status);

    List<RulePackRecord> findByStatusAndEffectiveFromLessThanEqual(String status, LocalDate onOrBefore);
}
