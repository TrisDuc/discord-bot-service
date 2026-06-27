package com.trisduc.triagebot.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestEventDTO(
        String action, // "opened", "synchronize", "closed"
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            @JsonProperty("number") Integer number,
            String title,
            @JsonProperty("html_url") String url,
            User user
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(@JsonProperty("login") String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(@JsonProperty("full_name") String fullName) {}
}

