package com.neurocalm.controller;

import com.neurocalm.dto.SessionEndDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.dto.SessionStartDTO;
import com.neurocalm.service.SessionService;
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
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionResponseDTO> start(@Valid @RequestBody SessionStartDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.startSession(request));
    }

    /** The request body is optional. */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<SessionResponseDTO> end(@PathVariable Long sessionId,
                                                  @RequestBody(required = false) SessionEndDTO request) {
        return ResponseEntity.ok(sessionService.endSession(sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponseDTO> get(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }
}
