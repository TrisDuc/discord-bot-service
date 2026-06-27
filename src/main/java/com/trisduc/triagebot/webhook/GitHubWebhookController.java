package com.trisduc.triagebot.webhook;

import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook/triage")
public class GitHubWebhookController {

    private final TriageService triageService;

    public GitHubWebhookController(TriageService triageService) {
        this.triageService = triageService;
    }

    // TODO: verify X-Hub-Signature-256 header trước khi xử lý (tạm bỏ qua cho giai đoạn test nội bộ)
    @PostMapping
    public ResponseEntity<Void> receiveTriageAlert(@RequestBody CodeRabbitWebhookRequest request) {
        triageService.handle(request);
        return ResponseEntity.accepted().build();
    }
}