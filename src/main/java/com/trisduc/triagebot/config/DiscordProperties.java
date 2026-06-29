package com.trisduc.triagebot.config;

import com.trisduc.triagebot.webhook.NotificationChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {

    private String defaultChannelId;
    private String githubFeedChannelId;
    private String prStatusChannelId;
    private String botAlertsChannelId;

    public String getDefaultChannelId() {
        return defaultChannelId;
    }

    public void setDefaultChannelId(String defaultChannelId) {
        this.defaultChannelId = defaultChannelId;
    }

    public String getGithubFeedChannelId() {
        return githubFeedChannelId;
    }

    public void setGithubFeedChannelId(String githubFeedChannelId) {
        this.githubFeedChannelId = githubFeedChannelId;
    }

    public String getPrStatusChannelId() {
        return prStatusChannelId;
    }

    public void setPrStatusChannelId(String prStatusChannelId) {
        this.prStatusChannelId = prStatusChannelId;
    }

    public String getBotAlertsChannelId() {
        return botAlertsChannelId;
    }

    public void setBotAlertsChannelId(String botAlertsChannelId) {
        this.botAlertsChannelId = botAlertsChannelId;
    }

    public String resolveChannelId(NotificationChannel channel) {
        return switch (channel) {
            case GITHUB_FEED -> firstNonBlank(githubFeedChannelId, defaultChannelId);
            case PR_STATUS -> firstNonBlank(prStatusChannelId, defaultChannelId);
            case BOT_ALERTS -> firstNonBlank(botAlertsChannelId, defaultChannelId);
        };
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
