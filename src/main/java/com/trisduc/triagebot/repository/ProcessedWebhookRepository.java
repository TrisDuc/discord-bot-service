package com.trisduc.triagebot.repository;

import com.trisduc.triagebot.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {
    Optional<ProcessedWebhook> findByGithubDeliveryId(String githubDeliveryId);

    // Cleanup old records
    @Modifying
    @Query("DELETE FROM ProcessedWebhook p WHERE p.createdAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}