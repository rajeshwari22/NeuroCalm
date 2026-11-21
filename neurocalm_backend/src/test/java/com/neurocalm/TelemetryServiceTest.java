package com.neurocalm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neurocalm.dto.LiveMonitoringDTO;
import com.neurocalm.dto.MotorStateDTO;
import com.neurocalm.dto.PpgDataDTO;
import com.neurocalm.dto.TelemetryRequest;
import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import com.neurocalm.enums.SessionStatus;
import com.neurocalm.exception.InvalidTelemetryException;
import com.neurocalm.exception.ResourceNotFoundException;
import com.neurocalm.model.MotorEvent;
import com.neurocalm.model.PpgReading;
import com.neurocalm.model.Session;
import com.neurocalm.repository.MotorEventRepository;
import com.neurocalm.repository.PpgReadingRepository;
import com.neurocalm.repository.SessionRepository;
import com.neurocalm.repository.SessionSummaryRepository;
import com.neurocalm.service.MotorCorrelationService;
import com.neurocalm.service.PpgProcessingService;
import com.neurocalm.service.SignalQualityService;
import com.neurocalm.service.TelemetryService;
import com.neurocalm.websocket.MonitoringBroadcaster;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private PpgReadingRepository ppgReadingRepository;
    @Mock private MotorEventRepository motorEventRepository;
    @Mock private SessionSummaryRepository summaryRepository;
    @Mock private MonitoringBroadcaster broadcaster;
    @Captor private ArgumentCaptor<List<PpgReading>> readingsCaptor;
    @Captor private ArgumentCaptor<List<MotorEvent>> eventsCaptor;

    private TelemetryService service;
    private Session session;
    private final Instant start = Instant.now().minusSeconds(20);

    @BeforeEach
    void setUp() {
        PpgProcessingService ppg = new PpgProcessingService();
        service = new TelemetryService(sessionRepository, ppgReadingRepository, motorEventRepository,
                summaryRepository, ppg, new SignalQualityService(ppg), new MotorCorrelationService(ppg), broadcaster);
        session = new Session();
        session.setId(1L);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartTime(Instant.now().minusSeconds(60));
    }

    /** 10 seconds of a 72 bpm pulse sampled at 25 Hz, with timestamps. */
    private List<PpgDataDTO> pulseSamples() {
        List<PpgDataDTO> samples = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            double value = 100 + 20 * Math.sin(2 * Math.PI * 1.2 * i / 25.0);
            samples.add(new PpgDataDTO(start.plusMillis(i * 40L), value, 0.1));
        }
        return samples;
    }

    private void sessionExists() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
    }

    @Test
    void processTelemetry_storesRawReadingsEstimatesHeartRateAndBroadcasts() {
        sessionExists();
        LiveMonitoringDTO live = service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), null, null));

        verify(ppgReadingRepository).saveAll(readingsCaptor.capture());
        assertEquals(250, readingsCaptor.getValue().size());
        assertEquals(100.0, readingsCaptor.getValue().get(0).getRawValue(), 1e-9); // raw value unchanged

        assertEquals("good", live.quality());
        assertTrue(live.heartRateEstimated());
        assertEquals(72, live.heartRate(), 6);
        assertEquals(250, live.ppg().size());
        assertEquals(250, live.bvp().size());
        assertEquals(3, live.motors().size());
        assertFalse(live.motorInterferenceSuspected());
        verify(broadcaster).broadcast(live);
    }

    @Test
    void processTelemetry_usesDeviceHeartRateWhenProvided() {
        sessionExists();
        LiveMonitoringDTO live = service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), null, 80));
        assertEquals(80, live.heartRate().intValue());
        assertFalse(live.heartRateEstimated());
    }

    @Test
    void processTelemetry_motorOnIsStoredAndReadingsAreFlagged() {
        sessionExists();
        List<MotorStateDTO> motors = List.of(new MotorStateDTO(MotorZone.LEFT, true, 60, "wave", start));

        LiveMonitoringDTO live = service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), motors, null));

        verify(motorEventRepository).saveAll(eventsCaptor.capture());
        assertEquals(1, eventsCaptor.getValue().size());
        assertEquals(ActivityType.MOTOR_ON, eventsCaptor.getValue().get(0).getActivityType());
        assertEquals(60, eventsCaptor.getValue().get(0).getPwmDuty());

        verify(ppgReadingRepository).saveAll(readingsCaptor.capture());
        assertTrue(readingsCaptor.getValue().stream().allMatch(PpgReading::isMotorActiveDuring));

        MotorStateDTO left = live.motors().get(0);
        assertEquals(MotorZone.LEFT, left.zone());
        assertTrue(left.active());
        assertEquals(60, left.pwmDuty());
    }

    @Test
    void processTelemetry_motorOffWithoutPreviousOnCreatesNoEvent() {
        sessionExists();
        List<MotorStateDTO> motors = List.of(new MotorStateDTO(MotorZone.RIGHT, false, 0, null, start));
        service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), motors, null));
        verify(motorEventRepository, never()).saveAll(any());
    }

    @Test
    void processTelemetry_invalidSamplesAreSkippedNotStored() {
        sessionExists();
        List<PpgDataDTO> samples = new ArrayList<>(pulseSamples());
        samples.set(3, new PpgDataDTO(start, null, null));
        samples.set(4, new PpgDataDTO(start, Double.NaN, null));

        service.processTelemetry(new TelemetryRequest(1L, samples, null, null));

        verify(ppgReadingRepository).saveAll(readingsCaptor.capture());
        assertEquals(248, readingsCaptor.getValue().size());
    }

    @Test
    void processTelemetry_allInvalidSamplesThrows() {
        sessionExists();
        List<PpgDataDTO> samples = Arrays.asList(new PpgDataDTO(start, null, null), new PpgDataDTO(start, null, null));
        assertThrows(InvalidTelemetryException.class,
                () -> service.processTelemetry(new TelemetryRequest(1L, samples, null, null)));
    }

    @Test
    void processTelemetry_unknownSessionThrowsNotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), null, null)));
    }

    @Test
    void processTelemetry_completedSessionIsRejected() {
        session.setStatus(SessionStatus.COMPLETED);
        sessionExists();
        assertThrows(InvalidTelemetryException.class,
                () -> service.processTelemetry(new TelemetryRequest(1L, pulseSamples(), null, null)));
    }
}
