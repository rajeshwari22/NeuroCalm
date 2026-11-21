package com.neurocalm.repository;

import com.neurocalm.model.Session;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserIdOrderByStartTimeDesc(Long userId);
    Optional<Session> findByIdAndUserId(Long id, Long userId);
}
