package com.neurocalm.dto;

import java.time.Instant;
import java.util.List;

/**
 * Sent over WebSocket after every telemetry batch.
 * Flutter-compatible fields: ppg, bvp, heartRate, quality (lowercase: good/moderate/poor).
 * ppg / bvp are MEASURED. heartRate is measured only when heartRateEstimated=false.
 * derivedPulse is DERIVED (PPG with its average removed). Nothing here is clinically validated.
 */
public record LiveMonitoringDTO(
        Long sessionId,
        List<Double> ppg,
        List<Double> bvp,
        Integer heartRate,
        boolean heartRateEstimated,
        String quality,
        List<MotorStateDTO> motors,
        boolean motorInterferenceSuspected,
        String motorInterferenceNote,
        List<Double> derivedPulse,
        long sessionDurationSeconds,
        Instant timestamp,
        String dataNote) { }
