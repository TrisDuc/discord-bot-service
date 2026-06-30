package com.trisduc.triagebot.webhook;

import com.trisduc.triagebot.config.DiscordProperties;
import com.trisduc.triagebot.entity.Member;
import com.trisduc.triagebot.entity.Project;
import com.trisduc.triagebot.repository.MemberRepository;
import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Map;
import java.util.Set;

@Service
public class DiscordNotifierService {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifierService.class);

    private final JDA jda;
    private final DiscordProperties discordProperties;
    private final MemberRepository memberRepository;

    public DiscordNotifierService(JDA jda, DiscordProperties discordProperties, MemberRepository memberRepository) {
        this.jda = jda;
        this.discordProperties = discordProperties;
        this.memberRepository = memberRepository;
    }

    public void sendTriageAlert(
            Project project,
            CodeRabbitWebhookRequest request,
            Set<String> modules,
            Set<Member> owners,
            Set<String> fallbackRoleIds
    ) {
        String mentions = buildMentions(owners, fallbackRoleIds);
        EmbedBuilder embed = buildEmbed(request, modules);

        String content = mentions.isBlank()
                ? embed.build().getTitle()
                : mentions;

        sendEmbed(discordProperties.resolveChannelId(NotificationChannel.BOT_ALERTS), content, embed, "triage alert");
    }

    public void sendNotification(NotificationChannel targetChannel, GitHubEventRouterService.DiscordMessage message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(message.title(), message.url())
                .setColor(message.color());

        if (message.description() != null) {
            embed.setDescription(message.description());
        }

        for (Map.Entry<String, String> field : message.fields().entrySet()) {
            embed.addField(field.getKey(), field.getValue(), true);
        }

        String mention = resolveMention(message.githubUsername());
        sendEmbed(discordProperties.resolveChannelId(targetChannel), mention, embed, targetChannel.name());
    }

    private String buildMentions(Set<Member> owners, Set<String> fallbackRoleIds) {
        StringBuilder sb = new StringBuilder();
        for (Member m : owners) {
            sb.append("<@").append(m.getDiscordUserId()).append("> ");
        }
        for (String roleId : fallbackRoleIds) {
            sb.append("<@&").append(roleId).append("> ");
        }
        return sb.toString().trim();
    }

    private void sendEmbed(String channelId, String content, EmbedBuilder embed, String logContext) {
        MessageChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warn("Discord channel {} not found for {}", channelId, logContext);
            return;
        }

        String safeContent = content == null || content.isBlank() ? "" : content;

        channel.sendMessage(safeContent)
                .addEmbeds(embed.build())
                .queue(
                        success -> log.info("Sent Discord notification to {} channel {}", logContext, channelId),
                        error -> log.error("Discord send failed for {}", logContext, error)
                );
    }

    private String resolveMention(String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) {
            return null;
        }

        return memberRepository.findByGithubUsernameIgnoreCase(githubUsername)
                .filter(Member::isActive)
                .map(Member::getDiscordUserId)
                .filter(discordUserId -> discordUserId != null && !discordUserId.isBlank())
                .map(discordUserId -> "<@" + discordUserId + ">")
                .orElse(null);
    }

    private EmbedBuilder buildEmbed(CodeRabbitWebhookRequest request, Set<String> modules) {
        boolean isFailure = "HIGH".equalsIgnoreCase(request.severity())
                || "CI_FAILED".equalsIgnoreCase(request.alertType());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("PR #" + request.prNumber() + " - " + request.prTitle(), request.prUrl())
                .setColor(isFailure ? Color.RED : new Color(46, 204, 113))
                .addField("Type", request.alertType(), true)
                .addField("Author", request.githubUsername(), true);

        if (!modules.isEmpty()) {
            embed.addField("Module", String.join(", ", modules), true);
        }
        if (request.severity() != null) {
            embed.addField("Severity", request.severity(), true);
        }
        if (request.summary() != null) {
            embed.setDescription(request.summary());
        }
        return embed;
    }
}
