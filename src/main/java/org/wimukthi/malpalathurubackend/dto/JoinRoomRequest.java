package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank(message = "Player name is required")
        String playerName,

        @NotBlank(message = "Room code is required")
        String roomCode
) {
}
