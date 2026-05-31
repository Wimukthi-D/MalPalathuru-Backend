package org.wimukthi.malpalathurubackend.dto;

import java.util.List;

public record LeaderboardResponse(
        String roomCode,
        Long roundId,
        Integer roundNumber,
        Boolean finalLeaderboard,
        List<LeaderboardEntryResponse> entries
) {
}
