package com.neurocalm.service;

import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import com.neurocalm.enums.SignalQuality;
import com.neurocalm.model.MotorEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Links PPG readings to motor activity (by timestamp) and gives a basic
 * "possible interference" assessment. It does NOT remove motion artifacts and
 * it never discards data - raw readings are always kept.
 */
@Service
public class MotorCorrelationService {

    private static final int MIN_BASELINE_SAMPLES = 10;
    private static final double INTERFERENCE_RATIO = 2.0;

    private final PpgProcessingService ppgProcessingService;

    public MotorCorrelationService(PpgProcessingService ppgProcessingService) {
        this.ppgProcessingService = ppgProcessingService;
    }

    /** Result of the interference check. */
    public record InterferenceAssessment(
            boolean motorActive,
            boolean interferenceSuspected,
            Double variabilityRatio,
            int maxPwmDuty,
            List<MotorZone> activeZones,
            String note) { }

    /**
     * Replays the motor events (sorted oldest first) up to time t and returns
     * the latest event of every motor that is ON at that moment.
     */
    public Map<MotorZone, MotorEvent> activeMotorsAt(Instant t, List<MotorEvent> events) {
        Map<MotorZone, MotorEvent> active = new EnumMap<>(MotorZone.class);
        for (MotorEvent event : events) {
            if (event.getEventTime().isAfter(t)) {
                continue;
            }
            if (event.getActivityType() == ActivityType.MOTOR_OFF || event.getPwmDuty() <= 0) {
                active.remove(event.getZone());
            } else {
                active.put(event.getZone(), event);
            }
        }
        return active;
    }

    /**
     * @param currentSamples  valid PPG samples of the current block
     * @param activeMotors    motors running now
     * @param quality         quality of the current block
     * @param baselineSamples recent samples recorded while no motor was running
     */
    public InterferenceAssessment assess(List<Double> currentSamples,
                                         Collection<MotorEvent> activeMotors,
                                         SignalQuality quality,
                                         List<Double> baselineSamples) {
        if (activeMotors.isEmpty()) {
            return new InterferenceAssessment(false, false, null, 0, List.of(),
                    "No motor active, no motor interference expected.");
        }
        List<MotorZone> zones = new ArrayList<>();
        int maxPwm = 0;
        for (MotorEvent motor : activeMotors) {
            zones.add(motor.getZone());
            maxPwm = Math.max(maxPwm, motor.getPwmDuty());
        }

        Double ratio = variabilityRatio(currentSamples, baselineSamples);
        boolean suspected;
        String note;
        if (ratio != null) {
            suspected = ratio >= INTERFERENCE_RATIO;
            note = suspected
                    ? "Possible motor interference: signal variability is about " + round(ratio)
                      + "x the no-motor baseline. Not confirmed; raw data kept."
                    : "Motor active, but signal variability is similar to the no-motor baseline.";
        } else {
            suspected = quality == SignalQuality.POOR;
            note = suspected
                    ? "Motor active and signal quality is poor; interference possible (no baseline yet)."
                    : "Motor active; no baseline yet, so interference cannot be assessed.";
        }
        return new InterferenceAssessment(true, suspected, ratio, maxPwm, zones, note);
    }

    /** Standard deviation of current samples divided by that of the baseline (null if unknown). */
    private Double variabilityRatio(List<Double> current, List<Double> baseline) {
        if (baseline == null || baseline.size() < MIN_BASELINE_SAMPLES || current.size() < MIN_BASELINE_SAMPLES) {
            return null;
        }
        double baselineDeviation = ppgProcessingService.standardDeviation(baseline);
        if (baselineDeviation < 1e-9) {
            return null;
        }
        return ppgProcessingService.standardDeviation(current) / baselineDeviation;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
