package com.aegispay.app.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stripe_event")
public class StripeEvent {

    @Id
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }
}
