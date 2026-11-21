package com.neurocalm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.neurocalm.dto.SessionEndDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.dto.SessionStartDTO;
import com.neurocalm.enums.MassageIntensity;
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
import com.neurocalm.service.SessionService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PpgReadingRepository ppgReadingRepository;
    @Mock private SessionSummaryRepository summaryRepository;

    private SessionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SessionService(sessionRepository, userRepository, ppgReadingRepository, summaryRepository);
        user = new User("Asha", "asha@example.com");
        user.setId(1L);
    }

    private Session sessionWithStatus(SessionStatus status) {
        Session s = new Session();
        s.setId(5L);
        s.setUser(user);
        s.setStatus(status);
        s.setStartTime(Instant.now().minusSeconds(120));
        return s;
    }

    @Test
    void startSession_createsActiveSessionWithSettings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SessionResponseDTO result = service.startSession(new SessionStartDTO(1L, MassageIntensity.LIGHT, "wave"));

        assertEquals(10L, result.id().longValue());
        assertEquals(1L, result.userId().longValue());
        assertEquals(SessionStatus.ACTIVE, result.status());
        assertEquals(MassageIntensity.LIGHT, result.intensity());
        assertEquals("wave", result.rhythm());
        assertNotNull(result.startTime());
        assertNull(result.summary());
    }

    @Test
    void startSession_unknownUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.startSession(new SessionStartDTO(99L, null, null)));
    }

    @Test
    void endSession_completesSessionAndBuildsSummary() {
        Session session = sessionWithStatus(SessionStatus.ACTIVE);
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(summaryRepository.save(any(SessionSummary.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ppgReadingRepository.countBySessionId(5L)).thenReturn(100L);
        when(ppgReadingRepository.countBySessionIdAndQuality(5L, SignalQuality.GOOD)).thenReturn(80L);
        when(ppgReadingRepository.countBySessionIdAndQuality(5L, SignalQuality.MODERATE)).thenReturn(15L);
        when(ppgReadingRepository.countBySessionIdAndQuality(5L, SignalQuality.POOR)).thenReturn(5L);
        when(ppgReadingRepository.countBySessionIdAndMotorActiveDuringTrue(5L)).thenReturn(40L);
        when(ppgReadingRepository.countBySessionIdAndMotorActiveDuringTrueAndQuality(5L, SignalQuality.POOR)).thenReturn(4L);
        when(ppgReadingRepository.findAverageHeartRate(5L)).thenReturn(72.4);
        when(ppgReadingRepository.findPeakHeartRate(5L)).thenReturn(85);

        SessionResponseDTO result = service.endSession(5L, new SessionEndDTO("felt calm"));

        assertEquals(SessionStatus.COMPLETED, result.status());
        assertNotNull(result.endTime());
        assertTrue(result.durationSeconds() >= 120);
        SessionResponseDTO.Summary summary = result.summary();
        assertEquals(100, summary.totalReadings());
        assertEquals(80, summary.goodReadings());
        assertEquals(40, summary.readingsDuringMotorActivity());
        assertEquals(4, summary.poorReadingsDuringMotor());
        assertEquals(72.4, summary.averageHeartRate(), 0.001);
        assertEquals(85, summary.peakHeartRate().intValue());
        assertTrue(summary.note().contains("felt calm"));
    }

    @Test
    void endSession_alreadyCompletedThrowsIllegalState() {
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionWithStatus(SessionStatus.COMPLETED)));
        assertThrows(IllegalStateException.class, () -> service.endSession(5L, null));
    }

    @Test
    void endSession_unknownSessionThrowsNotFound() {
        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.endSession(404L, null));
    }
}
