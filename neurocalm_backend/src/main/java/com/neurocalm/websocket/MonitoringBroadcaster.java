package com.neurocalm.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurocalm.dto.LiveMonitoringDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Converts live monitoring data to JSON and pushes it to all WebSocket clients. */
@Component
public class MonitoringBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MonitoringBroadcaster.class);

    private final ObjectMapper objectMapper;
    private final MonitoringWebSocketHandler handler;

    public MonitoringBroadcaster(ObjectMapper objectMapper, MonitoringWebSocketHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    public void broadcast(LiveMonitoringDTO data) {
        try {
            handler.broadcast(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            // A broadcast problem must never make the telemetry request fail.
            log.warn("Live broadcast failed: {}", e.getMessage());
        }
    }
}
