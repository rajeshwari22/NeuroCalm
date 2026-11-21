package com.neurocalm.controller;

import com.neurocalm.dto.SessionDetailDTO;
import com.neurocalm.dto.SessionResponseDTO;
import com.neurocalm.service.HistoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<SessionResponseDTO>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(historyService.getUserHistory(userId));
    }

    @GetMapping("/{userId}/{sessionId}")
    public ResponseEntity<SessionDetailDTO> getSessionDetail(@PathVariable Long userId,
                                                             @PathVariable Long sessionId) {
        return ResponseEntity.ok(historyService.getUserSessionDetail(userId, sessionId));
    }
}
