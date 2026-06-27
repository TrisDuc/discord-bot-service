package com.trisduc.triagebot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "github_username", nullable = false, unique = true)
    private String githubUsername;

    @Column(name = "discord_user_id", nullable = false, unique = true)
    private String discordUserId;

    @Column(name = "discord_role_id")
    private String discordRoleId;

    @Column(name = "team_role", nullable = false)
    private String teamRole; // BACKEND, FRONTEND, DEVOPS, TESTER

    @Column(nullable = false)
    private boolean active = true;
}