package com.trisduc.triagebot.repository;

import com.trisduc.triagebot.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}