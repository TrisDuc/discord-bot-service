package com.trisduc.triagebot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_full_name", nullable = false, unique = true)
    private String repoFullName; // "SciPub-TTS/backend"

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "project_context", columnDefinition = "TEXT")
    private String projectContext;

    @Column(name = "default_channel_id", nullable = false)
    private String defaultChannelId;
}