package com.trisduc.triagebot.webhook.dto;

import java.util.List;

public record CodeRabbitWebhookRequest(
        String repoFullName,
        Integer prNumber,
        String prTitle,
        String prUrl,
        String githubUsername,
        String alertType,
        String severity,
        String summary,
        List<String> changedFiles
) {}