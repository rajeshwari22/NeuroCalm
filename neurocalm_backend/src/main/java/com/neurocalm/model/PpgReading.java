package com.neurocalm.model;

import com.neurocalm.enums.SignalQuality;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * One PPG sample.
 * MEASURED: rawValue, bvp (if the device sent it), recordedAt.
 * DERIVED by the backend: quality, motorActiveDuring, heartRate (when heartRateEstimated is true).
 */
@Entity
@Table(name = "ppg_readings")
public class PpgReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "raw_value", nullable = false)
    private double rawValue;

    private Double bvp;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "heart_rate_estimated", nullable = false)
    private boolean heartRateEstimated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalQuality quality = SignalQuality.POOR;

    @Column(name = "motor_active_during", nullable = false)
    private boolean motorActiveDuring;

    public Long getId() { return id; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public double getRawValue() { return rawValue; }
    public void setRawValue(double rawValue) { this.rawValue = rawValue; }
    public Double getBvp() { return bvp; }
    public void setBvp(Double bvp) { this.bvp = bvp; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public boolean isHeartRateEstimated() { return heartRateEstimated; }
    public void setHeartRateEstimated(boolean heartRateEstimated) { this.heartRateEstimated = heartRateEstimated; }
    public SignalQuality getQuality() { return quality; }
    public void setQuality(SignalQuality quality) { this.quality = quality; }
    public boolean isMotorActiveDuring() { return motorActiveDuring; }
    public void setMotorActiveDuring(boolean motorActiveDuring) { this.motorActiveDuring = motorActiveDuring; }
}
