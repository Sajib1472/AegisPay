package com.aegispay.app.rules;

import com.aegispay.app.platform.identity.UserRole;
import com.aegispay.app.platform.tenancy.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rule-packs")
public class RulePackController {

    private final RulePackRepository packs;
    private final RulePackDocumentRepository documents;

    public RulePackController(RulePackRepository packs, RulePackDocumentRepository documents) {
        this.packs = packs;
        this.documents = documents;
    }

    @GetMapping
    public List<RulePackRecord> listPublished() {
        return packs.findByStatusOrderByJurisdictionAscVersionAsc("PUBLISHED");
    }

    @GetMapping("/{id}/documents")
    public List<RulePackDocument> listDocuments(@PathVariable UUID id) {
        return documents.findByRulePackId(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public RulePackRecord publish(@PathVariable UUID id, @RequestParam String status) {
        if (TenantContext.role() != null && !UserRole.PLATFORM_ADMIN.name().equals(TenantContext.role())) {
            throw new IllegalStateException("Only platform admin publishes rule packs");
        }
        RulePackRecord pack = packs.findById(id).orElseThrow();
        pack.setStatus(status.toUpperCase());
        return packs.save(pack);
    }
}
