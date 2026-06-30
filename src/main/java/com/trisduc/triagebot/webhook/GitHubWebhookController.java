package com.trisduc.triagebot.webhook;

import com.trisduc.triagebot.entity.ProcessedWebhook;
import com.trisduc.triagebot.repository.ProcessedWebhookRepository;
import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook/github")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final GitHubEventRouterService gitHubEventRouterService;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    @PostMapping("/{webhookType}")
    public ResponseEntity<?> webhookPost(
            @PathVariable String webhookType,  // bot-alert, feed, pr-status
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody String payload) {

        try {
            // 1. Check xem delivery_id đã xử lý chưa
            Optional<ProcessedWebhook> existing = processedWebhookRepository.findByGithubDeliveryId(deliveryId);
            if (existing.isPresent()) {
                log.info("Webhook {} event {} already processed, skipping", deliveryId, eventType);

                // Update status
                ProcessedWebhook record = existing.get();
                record.setStatus(ProcessedWebhook.Status.SKIPPED);
                record.setProcessedAt(LocalDateTime.now());
                processedWebhookRepository.save(record);

                return ResponseEntity.ok().body(Map.of("status", "skipped", "reason", "duplicate delivery"));
            }

            // 2. Xử lý webhook
            gitHubEventRouterService.handle(eventType, payload, webhookType);

            // 3. Lưu vào DB (successful)
            String payloadHash = hashPayload(payload);
            ProcessedWebhook record = ProcessedWebhook.builder()
                    .githubDeliveryId(deliveryId)
                    .eventType(eventType)
                    .webhookUrl(webhookType)
                    .payloadHash(payloadHash)
                    .status(ProcessedWebhook.Status.PROCESSED)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedWebhookRepository.save(record);

            log.info("Webhook {} event {} processed successfully", deliveryId, eventType);
            return ResponseEntity.ok().body(Map.of("status", "processed"));

        } catch (Exception e) {
            log.error("Error processing webhook {} event {}", deliveryId, eventType, e);

            // Lưu error vào DB
            ProcessedWebhook errorRecord = ProcessedWebhook.builder()
                    .githubDeliveryId(deliveryId)
                    .eventType(eventType)
                    .webhookUrl(webhookType)
                    .status(ProcessedWebhook.Status.ERROR)
                    .errorMessage(e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();
            processedWebhookRepository.save(errorRecord);

            return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String hashPayload(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().withUpperCase().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}