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
        List<PlayerResponse> players,
        Integer totalRounds,
        Integer currentRoundNumber,
        RoundResponse currentRound,
        List<RoundSummaryResponse> rounds,
        List<String> usedLetters
) {
    public RoomResponse(
            Long id,
            String roomCode,
            Language language,
            Integer maxPlayers,
            Integer countLimitSeconds,
            Boolean privateRoom,
            Boolean locked,
            RoomStatus status,
            List<PlayerResponse> players
    ) {
        this(
                id,
                roomCode,
                language,
                maxPlayers,
                countLimitSeconds,
                privateRoom,
                locked,
                status,
                players,
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }
}
