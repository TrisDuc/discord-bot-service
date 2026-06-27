package com.trisduc.triagebot.webhook;


import com.trisduc.triagebot.entity.Member;
import com.trisduc.triagebot.entity.Project;
import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Set;

@Service
public class DiscordNotifierService {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifierService.class);

    private final JDA jda;

    public DiscordNotifierService(JDA jda) {
        this.jda = jda;
    }

    public void sendTriageAlert(
            Project project,
            CodeRabbitWebhookRequest request,
            Set<String> modules,
            Set<Member> owners,
            Set<String> fallbackRoleIds
    ) {
        MessageChannel channel = jda.getTextChannelById(project.getDefaultChannelId());
        if (channel == null) {
            log.warn("Discord channel {} not found for project {}", project.getDefaultChannelId(), project.getProjectName());
            return;
        }

        String mentions = buildMentions(owners, fallbackRoleIds);
        EmbedBuilder embed = buildEmbed(request, modules);

        String content = mentions.isBlank()
                ? embed.build().getTitle()
                : mentions;

        channel.sendMessage(content)
                .addEmbeds(embed.build())
                .queue(
                        success -> log.info("Sent triage alert for PR #{}", request.prNumber()),
                        error -> log.error("Discord send failed for PR #{}", request.prNumber(), error)
                );
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

    private EmbedBuilder buildEmbed(CodeRabbitWebhookRequest request, Set<String> modules) {
        boolean isFailure = "HIGH".equalsIgnoreCase(request.severity())
                || "CI_FAILED".equalsIgnoreCase(request.alertType());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("PR #" + request.prNumber() + " — " + request.prTitle(), request.prUrl())
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