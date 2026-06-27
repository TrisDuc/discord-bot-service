package com.trisduc.triagebot.webhook;

import com.trisduc.triagebot.entity.Alert;
import com.trisduc.triagebot.entity.Project;
import com.trisduc.triagebot.repository.AlertRepository;
import com.trisduc.triagebot.repository.ProjectRepository;
import com.trisduc.triagebot.webhook.dto.CodeRabbitWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final ProjectRepository projectRepository;
    private final AlertRepository alertRepository;
    private final OwnershipResolver ownershipResolver;
    private final DiscordNotifierService discordNotifierService;

    @Transactional
    public void handle(CodeRabbitWebhookRequest request) {
        Project project = projectRepository.findByRepoFullName(request.repoFullName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Project not found for repo: " + request.repoFullName()));

        List<String> changedFiles = request.changedFiles() == null ? List.of() : request.changedFiles();

        OwnershipResolver.ResolvedOwnership ownership = ownershipResolver.resolve(project, changedFiles);

        Alert alert = Alert.builder()
                .project(project)
                .prNumber(request.prNumber())
                .githubUsername(request.githubUsername())
                .alertType(request.alertType())
                .severity(request.severity())
                .summary(request.summary())
                .status("OPEN")
                .files(String.join(",", changedFiles))
                .build();
        alertRepository.save(alert);

        try {
            discordNotifierService.sendTriageAlert(
                    project,
                    request,
                    ownership.modules(),
                    ownership.owners(),
                    ownership.fallbackRoleIds()
            );
        } catch (Exception e) {
            log.error("Failed to send triage alert to Discord for PR #{}", request.prNumber(), e);
        }
    }
}