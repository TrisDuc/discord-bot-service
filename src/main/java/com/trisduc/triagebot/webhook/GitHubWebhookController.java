package com.trisduc.triagebot.webhook;

import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook/triage")
public class GitHubWebhookController {

    private final TriageService triageService;
    private final GitHubEventRouterService gitHubEventRouterService;

    public GitHubWebhookController(TriageService triageService, GitHubEventRouterService gitHubEventRouterService) {
        this.triageService = triageService;
        this.gitHubEventRouterService = gitHubEventRouterService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveTriageAlert(@RequestBody CodeRabbitWebhookRequest request) {
        triageService.handle(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/github")
    public ResponseEntity<Void> receiveGitHubEvent(
            @RequestHeader(name = "X-GitHub-Event", defaultValue = "") String eventType,
            @RequestBody String payload
    ) {
        gitHubEventRouterService.handle(eventType, payload);
        return ResponseEntity.accepted().build();
    }
}
