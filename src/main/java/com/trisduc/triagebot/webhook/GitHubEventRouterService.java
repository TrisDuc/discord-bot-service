package com.trisduc.triagebot.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubEventRouterService {

    private static final Logger log = LoggerFactory.getLogger(GitHubEventRouterService.class);

    private final ObjectMapper objectMapper;
    private final DiscordNotifierService discordNotifierService;

    public void handle(String eventType, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            DiscordMessage message = buildMessage(eventType, root);

            if (message == null) {
                log.info("Skipping unsupported GitHub event {}", eventType);
                return;
            }

            discordNotifierService.sendNotification(resolveChannel(eventType, root), message);
        } catch (Exception e) {
            log.error("Failed to process GitHub event {}", eventType, e);
        }
    }

    private NotificationChannel resolveChannel(String eventType, JsonNode root) {
        if (isStatusEvent(eventType, root)) {
            return NotificationChannel.PR_STATUS;
        }

        if (isBotEvent(root)) {
            return NotificationChannel.BOT_ALERTS;
        }

        return NotificationChannel.GITHUB_FEED;
    }

    private boolean isStatusEvent(String eventType, JsonNode root) {
        if ("workflow_run".equals(eventType)
                || "check_run".equals(eventType)
                || "check_suite".equals(eventType)) {
            return true;
        }

        return "pull_request".equals(eventType)
                && "closed".equals(text(root, "action"));
    }

    private boolean isBotEvent(JsonNode root) {
        String senderLogin = text(root, "sender", "login");
        return senderLogin.endsWith("[bot]") || senderLogin.startsWith("coderabbit");
    }

    private DiscordMessage buildMessage(String eventType, JsonNode root) {
        return switch (eventType) {
            case "push" -> buildPushMessage(root);
            case "create" -> buildRefMessage(root, "Created");
            case "delete" -> buildRefMessage(root, "Deleted");
            case "pull_request" -> buildPullRequestMessage(root);
            case "workflow_run" -> buildWorkflowMessage(root);
            case "check_run" -> buildCheckRunMessage(root);
            case "check_suite" -> buildCheckSuiteMessage(root);
            default -> null;
        };
    }

    private DiscordMessage buildPushMessage(JsonNode root) {
        String branch = shortRef(text(root, "ref"));
        String headMessage = text(root, "head_commit", "message");

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Branch", branch);
        fields.put("Pusher", text(root, "pusher", "name"));
        fields.put("Commits", String.valueOf(root.path("commits").isArray() ? root.path("commits").size() : 0));

        return new DiscordMessage(
                "Push to " + branch,
                text(root, "compare"),
                emptyToNull(headMessage),
                new Color(52, 152, 219),
                fields
        );
    }

    private DiscordMessage buildRefMessage(JsonNode root, String verb) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Type", text(root, "ref_type"));
        fields.put("Name", text(root, "ref"));
        fields.put("Actor", text(root, "sender", "login"));

        return new DiscordMessage(
                verb + " " + text(root, "ref_type"),
                null,
                null,
                new Color(155, 89, 182),
                fields
        );
    }

    private DiscordMessage buildPullRequestMessage(JsonNode root) {
        JsonNode pr = root.path("pull_request");
        String action = text(root, "action");
        boolean merged = pr.path("merged").asBoolean(false);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Author", text(pr, "user", "login"));
        fields.put("Action", merged ? "merged" : action);
        fields.put("Branch", text(pr, "head", "ref"));

        String title = "PR #" + pr.path("number").asText("?") + " " + (merged ? "merged" : action);
        Color color = merged ? new Color(46, 204, 113) : new Color(241, 196, 15);

        return new DiscordMessage(
                title,
                text(pr, "html_url"),
                emptyToNull(text(pr, "title")),
                color,
                fields
        );
    }

    private DiscordMessage buildWorkflowMessage(JsonNode root) {
        JsonNode workflowRun = root.path("workflow_run");
        String conclusion = text(workflowRun, "conclusion");
        boolean success = "success".equalsIgnoreCase(conclusion);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Workflow", text(workflowRun, "name"));
        fields.put("Branch", text(workflowRun, "head_branch"));
        fields.put("Conclusion", emptyToDash(conclusion));

        return new DiscordMessage(
                "Workflow " + text(root, "action"),
                text(workflowRun, "html_url"),
                emptyToNull(text(workflowRun, "display_title")),
                success ? new Color(46, 204, 113) : Color.RED,
                fields
        );
    }

    private DiscordMessage buildCheckRunMessage(JsonNode root) {
        JsonNode checkRun = root.path("check_run");
        String conclusion = text(checkRun, "conclusion");
        boolean success = "success".equalsIgnoreCase(conclusion);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Check", text(checkRun, "name"));
        fields.put("Status", text(checkRun, "status"));
        fields.put("Conclusion", emptyToDash(conclusion));

        return new DiscordMessage(
                "Check run " + text(root, "action"),
                text(checkRun, "html_url"),
                null,
                success ? new Color(46, 204, 113) : Color.RED,
                fields
        );
    }

    private DiscordMessage buildCheckSuiteMessage(JsonNode root) {
        JsonNode checkSuite = root.path("check_suite");
        String conclusion = text(checkSuite, "conclusion");
        boolean success = "success".equalsIgnoreCase(conclusion);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Status", text(checkSuite, "status"));
        fields.put("Conclusion", emptyToDash(conclusion));
        fields.put("Branch", text(checkSuite, "head_branch"));

        return new DiscordMessage(
                "Check suite " + text(root, "action"),
                text(checkSuite, "check_runs_url"),
                null,
                success ? new Color(46, 204, 113) : Color.RED,
                fields
        );
    }

    private String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
        }
        return current.isMissingNode() || current.isNull() ? "" : current.asText("");
    }

    private String shortRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return "-";
        }
        int idx = ref.lastIndexOf('/');
        return idx >= 0 ? ref.substring(idx + 1) : ref;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public record DiscordMessage(
            String title,
            String url,
            String description,
            Color color,
            Map<String, String> fields
    ) {
    }
}
