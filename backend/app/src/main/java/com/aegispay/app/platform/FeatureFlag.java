package com.aegispay.app.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "platform", name = "feature_flag")
public class FeatureFlag {

    @Id
    private String code;

    @Column(nullable = false)
    private boolean enabled;

    private String note;

    public String getCode() {
        return code;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getNote() {
        return note;
    }
}
