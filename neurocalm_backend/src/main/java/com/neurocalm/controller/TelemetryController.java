package com.neurocalm.controller;

import com.neurocalm.dto.LiveMonitoringDTO;
import com.neurocalm.dto.SessionDetailDTO;
import com.neurocalm.dto.TelemetryRequest;
import com.neurocalm.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    /** Flutter forwards PPG + motor data here. */
    @PostMapping
    public ResponseEntity<LiveMonitoringDTO> receiveTelemetry(@Valid @RequestBody TelemetryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.processTelemetry(request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDetailDTO> getTelemetry(@PathVariable Long sessionId) {
        return ResponseEntity.ok(telemetryService.getTelemetry(sessionId));
    }
}
