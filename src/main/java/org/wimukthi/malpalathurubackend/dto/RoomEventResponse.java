package org.wimukthi.malpalathurubackend.dto;

import java.time.LocalDateTime;

public record RoomEventResponse(
        String type,
        String message,
        Long affectedPlayerId,
        RoomResponse room,
        Object payload,
        LocalDateTime timestamp
) {
    public RoomEventResponse(String type, String message, Long affectedPlayerId, RoomResponse room) {
        this(type, message, affectedPlayerId, room, null, LocalDateTime.now());
    }

    public RoomEventResponse(String type, String message, Long affectedPlayerId, RoomResponse room, Object payload) {
        this(type, message, affectedPlayerId, room, payload, LocalDateTime.now());
    }
}
