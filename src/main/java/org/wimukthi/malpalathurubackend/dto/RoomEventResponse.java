package org.wimukthi.malpalathurubackend.dto;

public record RoomEventResponse(
        String type,
        String message,
        Long affectedPlayerId,
        RoomResponse room
) {
}
