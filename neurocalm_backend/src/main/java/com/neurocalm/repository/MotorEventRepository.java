package com.neurocalm.repository;

import com.neurocalm.model.MotorEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotorEventRepository extends JpaRepository<MotorEvent, Long> {
    List<MotorEvent> findBySessionIdOrderByEventTimeAsc(Long sessionId);
}
