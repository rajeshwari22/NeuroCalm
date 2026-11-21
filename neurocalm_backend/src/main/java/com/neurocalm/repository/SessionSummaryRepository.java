package com.neurocalm.repository;

import com.neurocalm.model.SessionSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSummaryRepository extends JpaRepository<SessionSummary, Long> {
    Optional<SessionSummary> findBySessionId(Long sessionId);
}
