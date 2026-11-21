package com.neurocalm.websocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Keeps track of connected Flutter clients and sends text messages to all of them. */
@Component
public class MonitoringWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringWebSocketHandler.class);

    private final Set<WebSocketSession> clients = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        clients.add(session);
        log.info("Monitoring client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        clients.remove(session);
        log.info("Monitoring client disconnected: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        clients.remove(session);
    }

    /** Sends the JSON text to every open client. */
    public void broadcast(String json) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession client : clients) {
            if (!client.isOpen()) {
                clients.remove(client);
                continue;
            }
            try {
                synchronized (client) {   // a session can only send one message at a time
                    client.sendMessage(message);
                }
            } catch (IOException e) {
                log.warn("Could not send to client {}: {}", client.getId(), e.getMessage());
                clients.remove(client);
            }
        }
    }

    public int connectedClientCount() {
        return clients.size();
    }
}
