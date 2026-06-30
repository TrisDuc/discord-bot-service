package com.trisduc.triagebot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWebhook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_delivery_id", nullable = false, unique = true)
    private String githubDeliveryId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.PROCESSED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum Status {
        PROCESSED, SKIPPED, ERROR
    }
}