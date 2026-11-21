package com.neurocalm.dto;

import com.neurocalm.enums.MassageIntensity;
import com.neurocalm.enums.SessionStatus;
import com.neurocalm.model.Session;
import com.neurocalm.model.SessionSummary;
import java.time.Duration;
import java.time.Instant;

/** Session information returned by the API. "summary" is null until the session ends. */
public record SessionResponseDTO(
        Long id,
        Long userId,
        Instant startTime,
        Instant endTime,
        SessionStatus status,
        MassageIntensity intensity,
        String rhythm,
        Long durationSeconds,
        Summary summary) {

    public record Summary(
            long durationSeconds,
            long totalReadings,
            long goodReadings,
            long moderateReadings,
            long poorReadings,
            long readingsDuringMotorActivity,
            long poorReadingsDuringMotor,
            Double averageHeartRate,
            Integer peakHeartRate,
            String note) { }

    /** Converts entities to the response object. For active sessions the duration is "so far". */
    public static SessionResponseDTO from(Session s, SessionSummary summary) {
        Long duration = s.getDurationSeconds();
        if (duration == null) {
            duration = Duration.between(s.getStartTime(), Instant.now()).getSeconds();
        }
        Summary summaryDto = summary == null ? null : new Summary(
                summary.getDurationSeconds(), summary.getTotalReadings(), summary.getGoodReadings(),
                summary.getModerateReadings(), summary.getPoorReadings(),
                summary.getReadingsDuringMotorActivity(), summary.getPoorReadingsDuringMotor(),
                summary.getAverageHeartRate(), summary.getPeakHeartRate(), summary.getNote());
        return new SessionResponseDTO(s.getId(), s.getUser().getId(), s.getStartTime(), s.getEndTime(),
                s.getStatus(), s.getIntensity(), s.getRhythm(), duration, summaryDto);
    }
}
