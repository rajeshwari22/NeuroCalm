package com.neurocalm;

import static org.junit.jupiter.api.Assertions.*;

import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import com.neurocalm.enums.SignalQuality;
import com.neurocalm.model.MotorEvent;
import com.neurocalm.service.MotorCorrelationService;
import com.neurocalm.service.MotorCorrelationService.InterferenceAssessment;
import com.neurocalm.service.PpgProcessingService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotorCorrelationServiceTest {

    private final MotorCorrelationService service = new MotorCorrelationService(new PpgProcessingService());
    private final Instant t0 = Instant.parse("2025-01-01T10:00:00Z");

    private MotorEvent event(MotorZone zone, ActivityType type, int pwm, long secondsAfterStart) {
        MotorEvent e = new MotorEvent();
        e.setZone(zone);
        e.setActivityType(type);
        e.setPwmDuty(pwm);
        e.setEventTime(t0.plusSeconds(secondsAfterStart));
        return e;
    }

    /** Alternating samples: mean 100, swing of +/- amplitude. */
    private List<Double> swing(int count, double amplitude) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < count; i++) values.add(100 + (i % 2 == 0 ? amplitude : -amplitude));
        return values;
    }

    @Test
    void activeMotorsAt_followsOnAndOffEventsPerMotor() {
        List<MotorEvent> events = List.of(
                event(MotorZone.LEFT, ActivityType.MOTOR_ON, 50, 0),
                event(MotorZone.REAR, ActivityType.MOTOR_ON, 80, 5),
                event(MotorZone.LEFT, ActivityType.MOTOR_OFF, 0, 10));

        assertTrue(service.activeMotorsAt(t0.minusSeconds(1), events).isEmpty());
        assertEquals(1, service.activeMotorsAt(t0.plusSeconds(2), events).size());
        Map<MotorZone, MotorEvent> atSix = service.activeMotorsAt(t0.plusSeconds(6), events);
        assertTrue(atSix.containsKey(MotorZone.LEFT) && atSix.containsKey(MotorZone.REAR));
        Map<MotorZone, MotorEvent> atTwelve = service.activeMotorsAt(t0.plusSeconds(12), events);
        assertEquals(List.of(MotorZone.REAR), List.copyOf(atTwelve.keySet()));
    }

    @Test
    void activeMotorsAt_intensityChangeUpdatesPwm() {
        List<MotorEvent> events = List.of(
                event(MotorZone.RIGHT, ActivityType.MOTOR_ON, 30, 0),
                event(MotorZone.RIGHT, ActivityType.INTENSITY_CHANGE, 70, 4));
        assertEquals(70, service.activeMotorsAt(t0.plusSeconds(5), events).get(MotorZone.RIGHT).getPwmDuty());
    }

    @Test
    void assess_noMotorMeansNoInterference() {
        InterferenceAssessment result = service.assess(swing(50, 10), List.of(), SignalQuality.POOR, swing(50, 10));
        assertFalse(result.motorActive());
        assertFalse(result.interferenceSuspected());
    }

    @Test
    void assess_flagsInterferenceWhenVariabilityMuchHigherThanBaseline() {
        MotorEvent motor = event(MotorZone.LEFT, ActivityType.MOTOR_ON, 60, 0);
        InterferenceAssessment result = service.assess(swing(50, 50), List.of(motor), SignalQuality.MODERATE, swing(50, 10));
        assertTrue(result.motorActive());
        assertTrue(result.interferenceSuspected());
        assertEquals(5.0, result.variabilityRatio(), 0.01);
        assertEquals(60, result.maxPwmDuty());
        assertEquals(List.of(MotorZone.LEFT), result.activeZones());
    }

    @Test
    void assess_doesNotFlagWhenSignalSimilarToBaseline() {
        MotorEvent motor = event(MotorZone.REAR, ActivityType.MOTOR_ON, 60, 0);
        InterferenceAssessment result = service.assess(swing(50, 11), List.of(motor), SignalQuality.GOOD, swing(50, 10));
        assertTrue(result.motorActive());
        assertFalse(result.interferenceSuspected());
    }

    @Test
    void assess_withoutBaselineOnlyPoorQualityIsFlagged() {
        MotorEvent motor = event(MotorZone.LEFT, ActivityType.MOTOR_ON, 60, 0);
        assertTrue(service.assess(swing(50, 10), List.of(motor), SignalQuality.POOR, List.of()).interferenceSuspected());
        assertFalse(service.assess(swing(50, 10), List.of(motor), SignalQuality.GOOD, List.of()).interferenceSuspected());
    }
}
