package com.neurocalm.model;

import com.neurocalm.enums.ActivityType;
import com.neurocalm.enums.MotorZone;
import jakarta.persistence.*;
import java.time.Instant;

/** A change in a motor's state (turned on, turned off, or intensity changed). */
@Entity
@Table(name = "motor_events")
public class MotorEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotorZone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    /** PWM duty cycle in percent (0-100). */
    @Column(name = "pwm_duty", nullable = false)
    private int pwmDuty;

    private String rhythm;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    public Long getId() { return id; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public MotorZone getZone() { return zone; }
    public void setZone(MotorZone zone) { this.zone = zone; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public int getPwmDuty() { return pwmDuty; }
    public void setPwmDuty(int pwmDuty) { this.pwmDuty = pwmDuty; }
    public String getRhythm() { return rhythm; }
    public void setRhythm(String rhythm) { this.rhythm = rhythm; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
}
