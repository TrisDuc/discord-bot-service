package com.trisduc.triagebot.webhook;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

        if (isBotEvent(root) || isBotReviewEvent(eventType, root)) {
            return NotificationChannel.BOT_ALERTS;
        }

        return NotificationChannel.GITHUB_FEED;
    }

    private boolean isStatusEvent(String eventType, JsonNode root) {
        if ("workflow_run".equals(eventType)) {
            return "completed".equalsIgnoreCase(text(root, "action"));
        }

        return "pull_request".equals(eventType)
                && "closed".equals(text(root, "action"));
    }

    private boolean isBotEvent(JsonNode root) {
        String senderLogin = text(root, "sender", "login");
        return senderLogin.endsWith("[bot]") || senderLogin.startsWith("coderabbit");
    }

    private boolean isBotLogin(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }

        String normalized = login.toLowerCase();
        return normalized.endsWith("[bot]") || normalized.startsWith("coderabbit");
    }

    private boolean isCodeRabbitApp(String senderType, String senderLogin) {
        return "Bot".equalsIgnoreCase(senderType) && isBotLogin(senderLogin);
    }

    private boolean isBotReviewEvent(String eventType, JsonNode root) {
        return switch (eventType) {
            case "pull_request_review", "pull_request_review_comment" ->
                    isBotLogin(text(root, "review", "user", "login"))
                            || isBotLogin(text(root, "comment", "user", "login"))
                            || isCodeRabbitApp(text(root, "sender", "type"), text(root, "sender", "login"));
            case "issue_comment" ->
                    isPullRequestComment(root)
                            && (isBotLogin(text(root, "comment", "user", "login"))
                            || isCodeRabbitApp(text(root, "sender", "type"), text(root, "sender", "login")));
            default -> false;
        };
    }

    private boolean isPullRequestComment(JsonNode root) {
        return !text(root, "issue", "pull_request", "url").isBlank();
    }

    private DiscordMessage buildMessage(String eventType, JsonNode root) {
        return switch (eventType) {
            case "push" -> buildPushMessage(root);
            case "create" -> buildRefMessage(root, "Created");
            case "delete" -> buildRefMessage(root, "Deleted");
            case "pull_request" -> buildPullRequestMessage(root);
            case "pull_request_review" -> buildPullRequestReviewMessage(root);
            case "pull_request_review_comment" -> buildPullRequestReviewCommentMessage(root);
            case "issue_comment" -> buildIssueCommentMessage(root);
            case "workflow_run" -> buildWorkflowMessage(root);
            case "check_run", "check_suite" -> null;
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
                fields,
                text(root, "pusher", "name")
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
                fields,
                text(root, "sender", "login")
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

        String prTitle = text(pr, "title");
        String title = "PR #" + pr.path("number").asText("?")
                + (prTitle.isBlank() ? "" : " - " + prTitle);
        Color color = merged ? new Color(46, 204, 113) : new Color(241, 196, 15);

        return new DiscordMessage(
                title,
                text(pr, "html_url"),
                emptyToNull(buildPullRequestDescription(action, pr)),
                color,
                fields,
                text(pr, "user", "login")
        );
    }

    private String buildPullRequestDescription(String action, JsonNode pr) {
        String author = text(pr, "user", "login");
        if (author.isBlank()) {
            return "Pull request " + action;
        }

        return switch (action) {
            case "opened" -> "New pull request opened by " + author;
            case "edited" -> "Pull request updated by " + author;
            case "reopened" -> "Pull request reopened by " + author;
            case "synchronize" -> "New commits pushed by " + author;
            case "closed" -> pr.path("merged").asBoolean(false)
                    ? "Pull request merged by " + author
                    : "Pull request closed by " + author;
            default -> "Pull request " + action + " by " + author;
        };
    }

    private DiscordMessage buildWorkflowMessage(JsonNode root) {
        JsonNode workflowRun = root.path("workflow_run");
        String conclusion = text(workflowRun, "conclusion");
        boolean success = "success".equalsIgnoreCase(conclusion);
        String statusTitle = success ? "CI passed" : "CI failed";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Workflow", text(workflowRun, "name"));
        fields.put("Branch", text(workflowRun, "head_branch"));
        fields.put("Conclusion", emptyToDash(conclusion));

        return new DiscordMessage(
                statusTitle,
                text(workflowRun, "html_url"),
                emptyToNull(text(workflowRun, "display_title")),
                success ? new Color(46, 204, 113) : Color.RED,
                fields,
                text(root, "sender", "login")
        );
    }

    private DiscordMessage buildPullRequestReviewMessage(JsonNode root) {
        JsonNode pr = root.path("pull_request");
        JsonNode review = root.path("review");
        String action = text(root, "action");
        String reviewer = firstNonBlank(
                text(review, "user", "login"),
                text(root, "sender", "login")
        );

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Reviewer", reviewer);
        fields.put("State", emptyToDash(text(review, "state")));
        fields.put("Action", emptyToDash(action));

        return new DiscordMessage(
                "PR #" + pr.path("number").asText("?") + " review - " + text(pr, "title"),
                text(pr, "html_url"),
                emptyToNull(text(review, "body")),
                new Color(230, 126, 34),
                fields,
                reviewer
        );
    }

    private DiscordMessage buildPullRequestReviewCommentMessage(JsonNode root) {
        JsonNode pr = root.path("pull_request");
        JsonNode comment = root.path("comment");
        String reviewer = firstNonBlank(
                text(comment, "user", "login"),
                text(root, "sender", "login")
        );

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Reviewer", reviewer);
        fields.put("File", emptyToDash(text(comment, "path")));
        fields.put("Action", emptyToDash(text(root, "action")));

        return new DiscordMessage(
                "PR #" + pr.path("number").asText("?") + " review comment",
                firstNonBlank(text(comment, "html_url"), text(pr, "html_url")),
                emptyToNull(text(comment, "body")),
                new Color(230, 126, 34),
                fields,
                reviewer
        );
    }

    private DiscordMessage buildIssueCommentMessage(JsonNode root) {
        if (!isPullRequestComment(root)) {
            return null;
        }

        JsonNode issue = root.path("issue");
        JsonNode comment = root.path("comment");
        String commenter = firstNonBlank(
                text(comment, "user", "login"),
                text(root, "sender", "login")
        );

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Repository", text(root, "repository", "full_name"));
        fields.put("Commenter", commenter);
        fields.put("Action", emptyToDash(text(root, "action")));

        return new DiscordMessage(
                "PR #" + issue.path("number").asText("?") + " comment - " + text(issue, "title"),
                firstNonBlank(text(comment, "html_url"), text(issue, "html_url")),
                emptyToNull(text(comment, "body")),
                isBotReviewEvent("issue_comment", root) ? new Color(230, 126, 34) : new Color(52, 152, 219),
                fields,
                commenter
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
                fields,
                text(root, "sender", "login")
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
                fields,
                text(root, "sender", "login")
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public record DiscordMessage(
            String title,
            String url,
            String description,
            Color color,
            Map<String, String> fields,
            String githubUsername
    ) {
    }
}
