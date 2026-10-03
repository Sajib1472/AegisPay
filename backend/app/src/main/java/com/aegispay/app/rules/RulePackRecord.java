package com.aegispay.app.rules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "rule_pack")
public class RulePackRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String jurisdiction;

    @Column(nullable = false)
    private String version;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private String sha256;

    @Column(nullable = false)
    private String status;

    @Column(name = "engine_module", nullable = false)
    private String engineModule;

    public UUID getId() {
        return id;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getVersion() {
        return version;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public String getStatus() {
        return status;
    }

    public String getEngineModule() {
        return engineModule;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEngineModule(String engineModule) {
        this.engineModule = engineModule;
    }
}
