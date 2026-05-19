package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.Language;
import org.wimukthi.malpalathurubackend.enums.RoomStatus;

import java.util.List;

public record RoomResponse(
        Long id,
        String roomCode,
        Language language,
        Integer maxPlayers,
        Integer countLimitSeconds,
        Boolean privateRoom,
        Boolean locked,
        RoomStatus status,
        List <PlayerResponse> players
) {
}
