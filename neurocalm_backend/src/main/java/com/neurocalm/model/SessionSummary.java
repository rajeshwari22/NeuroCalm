package com.neurocalm.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Summary generated when a session ends. */
@Entity
@Table(name = "session_summaries")
public class SessionSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;
    @Column(name = "total_readings", nullable = false)
    private long totalReadings;
    @Column(name = "good_readings", nullable = false)
    private long goodReadings;
    @Column(name = "moderate_readings", nullable = false)
    private long moderateReadings;
    @Column(name = "poor_readings", nullable = false)
    private long poorReadings;
    @Column(name = "readings_during_motor_activity", nullable = false)
    private long readingsDuringMotorActivity;
    @Column(name = "poor_readings_during_motor", nullable = false)
    private long poorReadingsDuringMotor;
    @Column(name = "average_heart_rate")
    private Double averageHeartRate;
    @Column(name = "peak_heart_rate")
    private Integer peakHeartRate;
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long v) { this.durationSeconds = v; }
    public long getTotalReadings() { return totalReadings; }
    public void setTotalReadings(long v) { this.totalReadings = v; }
    public long getGoodReadings() { return goodReadings; }
    public void setGoodReadings(long v) { this.goodReadings = v; }
    public long getModerateReadings() { return moderateReadings; }
    public void setModerateReadings(long v) { this.moderateReadings = v; }
    public long getPoorReadings() { return poorReadings; }
    public void setPoorReadings(long v) { this.poorReadings = v; }
    public long getReadingsDuringMotorActivity() { return readingsDuringMotorActivity; }
    public void setReadingsDuringMotorActivity(long v) { this.readingsDuringMotorActivity = v; }
    public long getPoorReadingsDuringMotor() { return poorReadingsDuringMotor; }
    public void setPoorReadingsDuringMotor(long v) { this.poorReadingsDuringMotor = v; }
    public Double getAverageHeartRate() { return averageHeartRate; }
    public void setAverageHeartRate(Double v) { this.averageHeartRate = v; }
    public Integer getPeakHeartRate() { return peakHeartRate; }
    public void setPeakHeartRate(Integer v) { this.peakHeartRate = v; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
