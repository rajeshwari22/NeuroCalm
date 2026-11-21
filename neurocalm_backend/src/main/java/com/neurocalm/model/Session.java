package com.neurocalm.model;

import com.neurocalm.enums.MassageIntensity;
import com.neurocalm.enums.SessionStatus;
import jakarta.persistence.*;
import java.time.Instant;

/** One massage session of one user. */
@Entity
@Table(name = "massage_sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    private MassageIntensity intensity;

    private String rhythm;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public MassageIntensity getIntensity() { return intensity; }
    public void setIntensity(MassageIntensity intensity) { this.intensity = intensity; }
    public String getRhythm() { return rhythm; }
    public void setRhythm(String rhythm) { this.rhythm = rhythm; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}
