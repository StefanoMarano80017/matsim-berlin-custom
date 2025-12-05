package org.springboot.websocket;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


@Slf4j(topic = "org.springboot")
@Service
public class SimulationWebSocketService implements SimulationEventPublisher {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper mapper = new ObjectMapper();


    public void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
    }

    /**
     * Invia un evento JSON a tutti i client connessi
     */
    @Override
    public void publish(Object event) {
        try {
            String json = mapper.writeValueAsString(event);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            // logga eventualmente...
            log.error("Errore invio evento WebSocket: " + e.getMessage());
        }

        log.info("Evento pubblicato con successo" + event.toString());
    }
}
