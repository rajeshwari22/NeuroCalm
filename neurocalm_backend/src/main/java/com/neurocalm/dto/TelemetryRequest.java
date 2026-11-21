package com.neurocalm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Body of POST /api/telemetry. "motors" and "heartRate" are optional. */
public record TelemetryRequest(
        @NotNull Long sessionId,
        @NotEmpty List<@Valid PpgDataDTO> ppg,
        List<@Valid MotorStateDTO> motors,
        Integer heartRate) { }
