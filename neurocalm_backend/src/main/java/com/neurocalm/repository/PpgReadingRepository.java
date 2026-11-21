package com.neurocalm.repository;

import com.neurocalm.enums.SignalQuality;
import com.neurocalm.model.PpgReading;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PpgReadingRepository extends JpaRepository<PpgReading, Long> {

    List<PpgReading> findBySessionIdOrderByRecordedAtAsc(Long sessionId);

    /** Recent readings taken while no motor was running (used as a baseline). */
    List<PpgReading> findTop200BySessionIdAndMotorActiveDuringFalseOrderByRecordedAtDesc(Long sessionId);

    long countBySessionId(Long sessionId);
    long countBySessionIdAndQuality(Long sessionId, SignalQuality quality);
    long countBySessionIdAndMotorActiveDuringTrue(Long sessionId);
    long countBySessionIdAndMotorActiveDuringTrueAndQuality(Long sessionId, SignalQuality quality);

    @Query("SELECT AVG(r.heartRate) FROM PpgReading r WHERE r.session.id = :sessionId")
    Double findAverageHeartRate(@Param("sessionId") Long sessionId);

    @Query("SELECT MAX(r.heartRate) FROM PpgReading r WHERE r.session.id = :sessionId")
    Integer findPeakHeartRate(@Param("sessionId") Long sessionId);
}
