package com.trisduc.triagebot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ownership_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "path_pattern", nullable = false)
    private String pathPattern; // "src/main/java/**"

    @Column(name = "module_name", nullable = false)
    private String moduleName; // "Backend"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id")
    private Member ownerMember;

    @Column(name = "owner_role_id")
    private String ownerRoleId;
}