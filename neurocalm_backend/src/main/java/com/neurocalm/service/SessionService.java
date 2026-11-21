package com.neurocalm.service;

import com.neurocalm.dto.SessionEndDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.dto.SessionStartDTO;
import com.neurocalm.enums.SessionStatus;
import com.neurocalm.enums.SignalQuality;
import com.neurocalm.exception.ResourceNotFoundException;
import com.neurocalm.model.Session;
import com.neurocalm.model.SessionSummary;
import com.neurocalm.model.User;
import com.neurocalm.repository.PpgReadingRepository;
import com.neurocalm.repository.SessionRepository;
import com.neurocalm.repository.SessionSummaryRepository;
import com.neurocalm.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts and ends massage sessions and builds the session summary. */
@Service
public class SessionService {

    private static final String SUMMARY_NOTE =
            "Heart rate values may be estimated and are not clinically validated.";

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final PpgReadingRepository ppgReadingRepository;
    private final SessionSummaryRepository summaryRepository;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository,
                          PpgReadingRepository ppgReadingRepository,
                          SessionSummaryRepository summaryRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.ppgReadingRepository = ppgReadingRepository;
        this.summaryRepository = summaryRepository;
    }

    @Transactional
    public SessionResponseDTO startSession(SessionStartDTO request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        Session session = new Session();
        session.setUser(user);
        session.setStartTime(Instant.now());   // session start time
        session.setStatus(SessionStatus.ACTIVE);
        session.setIntensity(request.intensity());
        session.setRhythm(request.rhythm());
        Session saved = sessionRepository.save(session);
        return SessionResponseDTO.from(saved, null);
    }

    @Transactional
    public SessionResponseDTO endSession(Long sessionId, SessionEndDTO request) {
        Session session = findSession(sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session " + sessionId + " has already ended");
        }
        Instant endTime = Instant.now();
        session.setEndTime(endTime);
        session.setDurationSeconds(Duration.between(session.getStartTime(), endTime).getSeconds());
        session.setStatus(SessionStatus.COMPLETED);
        Session saved = sessionRepository.save(session);

        SessionSummary summary = summaryRepository.save(buildSummary(saved, request));
        return SessionResponseDTO.from(saved, summary);
    }

    @Transactional(readOnly = true)
    public SessionResponseDTO getSession(Long sessionId) {
        Session session = findSession(sessionId);
        SessionSummary summary = summaryRepository.findBySessionId(sessionId).orElse(null);
        return SessionResponseDTO.from(session, summary);
    }

    /** Counts readings by quality and calculates heart-rate statistics. */
    private SessionSummary buildSummary(Session session, SessionEndDTO request) {
        Long id = session.getId();
        SessionSummary summary = new SessionSummary();
        summary.setSession(session);
        summary.setDurationSeconds(session.getDurationSeconds());
        summary.setTotalReadings(ppgReadingRepository.countBySessionId(id));
        summary.setGoodReadings(ppgReadingRepository.countBySessionIdAndQuality(id, SignalQuality.GOOD));
        summary.setModerateReadings(ppgReadingRepository.countBySessionIdAndQuality(id, SignalQuality.MODERATE));
        summary.setPoorReadings(ppgReadingRepository.countBySessionIdAndQuality(id, SignalQuality.POOR));
        summary.setReadingsDuringMotorActivity(ppgReadingRepository.countBySessionIdAndMotorActiveDuringTrue(id));
        summary.setPoorReadingsDuringMotor(
                ppgReadingRepository.countBySessionIdAndMotorActiveDuringTrueAndQuality(id, SignalQuality.POOR));
        summary.setAverageHeartRate(ppgReadingRepository.findAverageHeartRate(id));
        summary.setPeakHeartRate(ppgReadingRepository.findPeakHeartRate(id));
        String userNote = request == null ? null : request.note();
        summary.setNote(userNote == null || userNote.isBlank() ? SUMMARY_NOTE : userNote + " | " + SUMMARY_NOTE);
        return summary;
    }

    private Session findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }
}
