package com.neurocalm.service;

import com.neurocalm.dto.LiveMonitoringDTO;
import com.neurocalm.dto.MotorStateDTO;
import com.neurocalm.dto.PpgDataDTO;
import com.neurocalm.dto.SessionDetailDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.dto.TelemetryRequest;
import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import com.neurocalm.enums.SessionStatus;
import com.neurocalm.enums.SignalQuality;
import com.neurocalm.exception.InvalidTelemetryException;
import com.neurocalm.exception.ResourceNotFoundException;
import com.neurocalm.model.MotorEvent;
import com.neurocalm.model.PpgReading;
import com.neurocalm.model.Session;
import com.neurocalm.model.SessionSummary;
import com.neurocalm.repository.MotorEventRepository;
import com.neurocalm.repository.PpgReadingRepository;
import com.neurocalm.repository.SessionRepository;
import com.neurocalm.repository.SessionSummaryRepository;
import com.neurocalm.websocket.MonitoringBroadcaster;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Receives telemetry from Flutter, stores it, analyses it and broadcasts the result. */
@Service
public class TelemetryService {

    private static final int MIN_PLAUSIBLE_HEART_RATE = 30;
    private static final int MAX_PLAUSIBLE_HEART_RATE = 220;
    private static final long DEFAULT_SAMPLE_SPACING_MS = 40; // used when timestamps are missing (25 Hz)
    private static final String DATA_NOTE =
            "ppg/bvp are measured. heartRate is estimated when heartRateEstimated=true. "
            + "derivedPulse is calculated. Not clinically validated.";

    private final SessionRepository sessionRepository;
    private final PpgReadingRepository ppgReadingRepository;
    private final MotorEventRepository motorEventRepository;
    private final SessionSummaryRepository summaryRepository;
    private final PpgProcessingService ppgProcessingService;
    private final SignalQualityService signalQualityService;
    private final MotorCorrelationService motorCorrelationService;
    private final MonitoringBroadcaster broadcaster;

    public TelemetryService(SessionRepository sessionRepository,
                            PpgReadingRepository ppgReadingRepository,
                            MotorEventRepository motorEventRepository,
                            SessionSummaryRepository summaryRepository,
                            PpgProcessingService ppgProcessingService,
                            SignalQualityService signalQualityService,
                            MotorCorrelationService motorCorrelationService,
                            MonitoringBroadcaster broadcaster) {
        this.sessionRepository = sessionRepository;
        this.ppgReadingRepository = ppgReadingRepository;
        this.motorEventRepository = motorEventRepository;
        this.summaryRepository = summaryRepository;
        this.ppgProcessingService = ppgProcessingService;
        this.signalQualityService = signalQualityService;
        this.motorCorrelationService = motorCorrelationService;
        this.broadcaster = broadcaster;
    }

    /** Main flow: validate -> store motor events -> store PPG -> analyse -> broadcast. */
    @Transactional
    public LiveMonitoringDTO processTelemetry(TelemetryRequest request) {
        Session session = findActiveSession(request.sessionId());

        List<Double> rawValues = request.ppg().stream().map(PpgDataDTO::value).toList();
        List<Double> validValues = ppgProcessingService.cleanSamples(rawValues);
        if (validValues.isEmpty()) {
            throw new InvalidTelemetryException("Request contains no valid PPG samples");
        }

        // 1. motor events (needed first, so PPG timestamps can be matched against them)
        List<MotorEvent> events = new ArrayList<>(
                motorEventRepository.findBySessionIdOrderByEventTimeAsc(session.getId()));
        saveMotorEvents(session, request.motors(), events);

        // 2. PPG readings (raw values are stored unchanged)
        List<PpgReading> readings = buildReadings(session, request.ppg(), events);
        boolean motorActiveInBlock = readings.stream().anyMatch(PpgReading::isMotorActiveDuring);

        // 3. analysis
        SignalQuality quality = signalQualityService.assess(rawValues, motorActiveInBlock);
        Integer heartRate = request.heartRate();
        boolean estimated = false;
        if (!isPlausibleHeartRate(heartRate)) {
            heartRate = estimateHeartRate(readings, validValues, quality);
            estimated = heartRate != null;
        }
        for (PpgReading reading : readings) {
            reading.setQuality(quality);
            reading.setHeartRate(heartRate);
            reading.setHeartRateEstimated(estimated);
        }
        ppgReadingRepository.saveAll(readings);

        // 4. motor-aware assessment
        Instant lastTime = readings.get(readings.size() - 1).getRecordedAt();
        Map<MotorZone, MotorEvent> activeMotors = motorCorrelationService.activeMotorsAt(lastTime, events);
        List<Double> baseline = ppgReadingRepository
                .findTop200BySessionIdAndMotorActiveDuringFalseOrderByRecordedAtDesc(session.getId())
                .stream().map(PpgReading::getRawValue).toList();
        MotorCorrelationService.InterferenceAssessment interference =
                motorCorrelationService.assess(validValues, activeMotors.values(), quality, baseline);

        // 5. build + broadcast the live response
        LiveMonitoringDTO live = buildLiveResponse(session, readings, validValues, quality, heartRate,
                estimated, activeMotors, interference, lastTime);
        broadcaster.broadcast(live);
        return live;
    }

    /** All stored telemetry (PPG + motor events) of a session. */
    @Transactional(readOnly = true)
    public SessionDetailDTO getTelemetry(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        SessionSummary summary = summaryRepository.findBySessionId(sessionId).orElse(null);

        List<SessionDetailDTO.PpgItem> ppgItems = ppgReadingRepository
                .findBySessionIdOrderByRecordedAtAsc(sessionId).stream()
                .map(r -> new SessionDetailDTO.PpgItem(r.getRecordedAt(), r.getRawValue(), r.getBvp(),
                        r.getHeartRate(), r.isHeartRateEstimated(), r.getQuality(), r.isMotorActiveDuring()))
                .toList();
        List<SessionDetailDTO.MotorEventItem> motorItems = motorEventRepository
                .findBySessionIdOrderByEventTimeAsc(sessionId).stream()
                .map(e -> new SessionDetailDTO.MotorEventItem(e.getEventTime(), e.getZone(),
                        e.getActivityType(), e.getPwmDuty(), e.getRhythm()))
                .toList();
        return new SessionDetailDTO(SessionResponseDTO.from(session, summary), ppgItems, motorItems);
    }

    // ------------------------------------------------------------------ helpers

    private Session findActiveSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidTelemetryException("Session " + sessionId + " is not active");
        }
        return session;
    }

    /** Only stores an event when a motor actually changes state. */
    private void saveMotorEvents(Session session, List<MotorStateDTO> motors, List<MotorEvent> events) {
        if (motors == null || motors.isEmpty()) {
            return;
        }
        List<MotorEvent> newEvents = new ArrayList<>();
        for (MotorStateDTO motor : motors) {
            Instant time = motor.timestamp() != null ? motor.timestamp() : Instant.now();
            MotorEvent previous = motorCorrelationService.activeMotorsAt(time, events).get(motor.zone());
            boolean nowActive = motor.active() && motor.pwmDuty() > 0;
            ActivityType type = decideActivityType(previous, nowActive, motor.pwmDuty());
            if (type == null) {
                continue; // nothing changed
            }
            MotorEvent event = new MotorEvent();
            event.setSession(session);
            event.setZone(motor.zone());
            event.setActivityType(type);
            event.setPwmDuty(nowActive ? motor.pwmDuty() : 0);
            event.setRhythm(motor.rhythm());
            event.setEventTime(time);
            events.add(event);
            events.sort(Comparator.comparing(MotorEvent::getEventTime));
            newEvents.add(event);
        }
        if (!newEvents.isEmpty()) {
            motorEventRepository.saveAll(newEvents);
        }
    }

    private ActivityType decideActivityType(MotorEvent previous, boolean nowActive, int pwmDuty) {
        if (previous == null) {
            return nowActive ? ActivityType.MOTOR_ON : null;
        }
        if (!nowActive) {
            return ActivityType.MOTOR_OFF;
        }
        return previous.getPwmDuty() != pwmDuty ? ActivityType.INTENSITY_CHANGE : null;
    }

    /** Creates one PpgReading per valid sample and flags the ones recorded while a motor was on. */
    private List<PpgReading> buildReadings(Session session, List<PpgDataDTO> samples, List<MotorEvent> events) {
        List<PpgReading> readings = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < samples.size(); i++) {
            PpgDataDTO sample = samples.get(i);
            Double value = sample.value();
            if (value == null || value.isNaN() || value.isInfinite()) {
                continue; // invalid sample: skipped safely
            }
            Instant time = sample.timestamp() != null ? sample.timestamp()
                    : now.minusMillis((samples.size() - 1 - i) * DEFAULT_SAMPLE_SPACING_MS);
            PpgReading reading = new PpgReading();
            reading.setSession(session);
            reading.setRecordedAt(time);
            reading.setRawValue(value);
            reading.setBvp(sample.bvp());
            reading.setMotorActiveDuring(!motorCorrelationService.activeMotorsAt(time, events).isEmpty());
            readings.add(reading);
        }
        return readings;
    }

    private boolean isPlausibleHeartRate(Integer heartRate) {
        return heartRate != null && heartRate >= MIN_PLAUSIBLE_HEART_RATE && heartRate <= MAX_PLAUSIBLE_HEART_RATE;
    }

    /** Heart rate is not estimated from poor-quality signals. */
    private Integer estimateHeartRate(List<PpgReading> readings, List<Double> validValues, SignalQuality quality) {
        if (quality == SignalQuality.POOR) {
            return null;
        }
        List<Instant> times = readings.stream().map(PpgReading::getRecordedAt).toList();
        double sampleRate = ppgProcessingService.estimateSampleRate(times);
        return ppgProcessingService.estimateHeartRate(validValues, sampleRate);
    }

    private LiveMonitoringDTO buildLiveResponse(Session session, List<PpgReading> readings,
                                                List<Double> validValues, SignalQuality quality,
                                                Integer heartRate, boolean estimated,
                                                Map<MotorZone, MotorEvent> activeMotors,
                                                MotorCorrelationService.InterferenceAssessment interference,
                                                Instant lastTime) {
        List<Double> bvp = readings.stream().map(PpgReading::getBvp).filter(v -> v != null).toList();
        List<MotorStateDTO> motorStates = new ArrayList<>();
        for (MotorZone zone : MotorZone.values()) {   // always report all three motors
            MotorEvent event = activeMotors.get(zone);
            motorStates.add(event == null
                    ? new MotorStateDTO(zone, false, 0, null, lastTime)
                    : new MotorStateDTO(zone, true, event.getPwmDuty(), event.getRhythm(), event.getEventTime()));
        }
        long duration = Math.max(0, Duration.between(session.getStartTime(), lastTime).getSeconds());
        return new LiveMonitoringDTO(session.getId(), validValues, bvp, heartRate, estimated,
                quality.name().toLowerCase(), motorStates, interference.interferenceSuspected(),
                interference.note(), ppgProcessingService.derivePulse(validValues),
                duration, lastTime, DATA_NOTE);
    }
}
