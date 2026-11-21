package com.neurocalm.config;

import com.neurocalm.websocket.MonitoringWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registers the live monitoring WebSocket at /ws/monitoring. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MonitoringWebSocketHandler monitoringHandler;

    public WebSocketConfig(MonitoringWebSocketHandler monitoringHandler) {
        this.monitoringHandler = monitoringHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(monitoringHandler, "/ws/monitoring").setAllowedOriginPatterns("*");
    }
}
