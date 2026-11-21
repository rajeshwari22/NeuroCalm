package com.neurocalm.dto;

import com.neurocalm.enums.MotorZone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** State of one motor (used in requests and in live monitoring responses). */
public record MotorStateDTO(
        @NotNull MotorZone zone,
        boolean active,
        @Min(0) @Max(100) int pwmDuty,   // duty cycle in percent
        String rhythm,
        Instant timestamp) { }
