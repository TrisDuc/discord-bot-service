package com.trisduc.triagebot.repository;

import com.trisduc.triagebot.entity.OwnershipRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OwnershipRuleRepository extends JpaRepository<OwnershipRule, Long> {
    List<OwnershipRule> findByProjectId(Long projectId);
}