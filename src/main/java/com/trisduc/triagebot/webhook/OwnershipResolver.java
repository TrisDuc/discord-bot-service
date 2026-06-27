package com.trisduc.triagebot.webhook;


import com.trisduc.triagebot.entity.Member;
import com.trisduc.triagebot.entity.OwnershipRule;
import com.trisduc.triagebot.entity.Project;
import com.trisduc.triagebot.repository.OwnershipRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OwnershipResolver {

    private final OwnershipRuleRepository ownershipRuleRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    public record ResolvedOwnership(Set<String> modules, Set<Member> owners, Set<String> fallbackRoleIds) {}

    public ResolvedOwnership resolve(Project project, List<String> changedFiles) {
        List<OwnershipRule> rules = ownershipRuleRepository.findByProjectId(project.getId());

        Set<String> modules = new LinkedHashSet<>();
        Set<Member> owners = new LinkedHashSet<>();
        Set<String> fallbackRoleIds = new LinkedHashSet<>();

        for (String file : changedFiles) {
            for (OwnershipRule rule : rules) {
                if (pathMatcher.match(rule.getPathPattern(), file)) {
                    modules.add(rule.getModuleName());
                    if (rule.getOwnerMember() != null && rule.getOwnerMember().isActive()) {
                        owners.add(rule.getOwnerMember());
                    } else if (rule.getOwnerRoleId() != null) {
                        fallbackRoleIds.add(rule.getOwnerRoleId());
                    }
                    break;
                }
            }
        }
        return new ResolvedOwnership(modules, owners, fallbackRoleIds);
    }
}