package org.springboot.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "org.springboot")
@Component
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationWebSocketService publisher;

    public SimulationWebSocketHandler(SimulationWebSocketService publisher) {
        this.publisher = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        publisher.registerSession(session);
        log.info("Client connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        publisher.unregisterSession(session);
        log.info("Client disconnected: " + session.getId());
    }
}