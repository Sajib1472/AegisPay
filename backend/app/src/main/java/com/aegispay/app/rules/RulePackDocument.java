package com.aegispay.app.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(schema = "platform", name = "rule_pack_document")
public class RulePackDocument {

    @Id
    private UUID id;

    @Column(name = "rule_pack_id", nullable = false)
    private UUID rulePackId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String citation;

    @Column(name = "body_markdown", nullable = false)
    private String bodyMarkdown;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getRulePackId() {
        return rulePackId;
    }

    public void setRulePackId(UUID rulePackId) {
        this.rulePackId = rulePackId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public void setBodyMarkdown(String bodyMarkdown) {
        this.bodyMarkdown = bodyMarkdown;
    }
}
