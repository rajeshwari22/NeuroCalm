package com.neurocalm.dto;

import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import com.neurocalm.enums.SignalQuality;
import java.time.Instant;
import java.util.List;

/** A session together with all its stored PPG readings and motor events. */
public record SessionDetailDTO(
        SessionResponseDTO session,
        List<PpgItem> ppgReadings,
        List<MotorEventItem> motorEvents) {

    public record PpgItem(
            Instant timestamp, double rawValue, Double bvp, Integer heartRate,
            boolean heartRateEstimated, SignalQuality quality, boolean motorActiveDuring) { }

    public record MotorEventItem(
            Instant timestamp, MotorZone zone, ActivityType activityType, int pwmDuty, String rhythm) { }
}
