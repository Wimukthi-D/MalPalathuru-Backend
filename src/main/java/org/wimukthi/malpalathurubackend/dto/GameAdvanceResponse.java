package org.wimukthi.malpalathurubackend.dto;

public record GameAdvanceResponse(
        Boolean finished,
        RoomResponse room,
        LeaderboardResponse leaderboard
) {
}
