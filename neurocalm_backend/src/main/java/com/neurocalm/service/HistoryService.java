package com.neurocalm.service;

import com.neurocalm.dto.SessionDetailDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.exception.ResourceNotFoundException;
import com.neurocalm.model.Session;
import com.neurocalm.repository.SessionRepository;
import com.neurocalm.repository.SessionSummaryRepository;
import com.neurocalm.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to a user's past sessions. */
@Service
public class HistoryService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SessionSummaryRepository summaryRepository;
    private final TelemetryService telemetryService;

    public HistoryService(UserRepository userRepository, SessionRepository sessionRepository,
                          SessionSummaryRepository summaryRepository, TelemetryService telemetryService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.summaryRepository = summaryRepository;
        this.telemetryService = telemetryService;
    }

    /** All sessions of a user, newest first. */
    @Transactional(readOnly = true)
    public List<SessionResponseDTO> getUserHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return sessionRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(s -> SessionResponseDTO.from(s, summaryRepository.findBySessionId(s.getId()).orElse(null)))
                .toList();
    }

    /** One session (with PPG readings and motor events) - only if it belongs to the user. */
    @Transactional(readOnly = true)
    public SessionDetailDTO getUserSessionDetail(Long userId, Long sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session " + sessionId + " not found for user " + userId));
        return telemetryService.getTelemetry(session.getId());
    }
}
