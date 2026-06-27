package com.trisduc.triagebot.repository;

import com.trisduc.triagebot.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByRepoFullName(String repoFullName);
}