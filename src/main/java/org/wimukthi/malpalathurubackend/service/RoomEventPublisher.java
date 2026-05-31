package org.wimukthi.malpalathurubackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.wimukthi.malpalathurubackend.dto.RoomEventResponse;
import org.wimukthi.malpalathurubackend.dto.RoomResponse;

@Service
public class RoomEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoomEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RoomEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastState(String roomCode, String type, String message, Long affectedPlayerId, RoomResponse room) {
        broadcast("/topic/rooms/" + roomCode + "/state", type, message, affectedPlayerId, room, null);
    }

    public void broadcastState(String roomCode, String type, String message, Long affectedPlayerId, RoomResponse room, Object payload) {
        broadcast("/topic/rooms/" + roomCode + "/state", type, message, affectedPlayerId, room, payload);
    }

    public void broadcastTimer(String roomCode, String type, String message, RoomResponse room, Object payload) {
        broadcast("/topic/rooms/" + roomCode + "/timer", type, message, null, room, payload);
    }

    public void broadcastReview(String roomCode, String type, String message, Long affectedPlayerId, RoomResponse room, Object payload) {
        broadcast("/topic/rooms/" + roomCode + "/review", type, message, affectedPlayerId, room, payload);
    }

    public void broadcastLeaderboard(String roomCode, String type, String message, RoomResponse room, Object payload) {
        broadcast("/topic/rooms/" + roomCode + "/leaderboard", type, message, null, room, payload);
    }

    private void broadcast(String topic, String type, String message, Long affectedPlayerId, RoomResponse room, Object payload) {
        RoomEventResponse event = new RoomEventResponse(type, message, affectedPlayerId, room, payload);
        log.info("Broadcasting {} to {}", type, topic);
        messagingTemplate.convertAndSend(topic, event);
    }
}
