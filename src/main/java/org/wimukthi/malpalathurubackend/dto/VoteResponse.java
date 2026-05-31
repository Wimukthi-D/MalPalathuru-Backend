package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.VoteValue;

import java.time.LocalDateTime;

public record VoteResponse(
        Long playerId,
        String playerName,
        VoteValue vote,
        LocalDateTime votedAt
) {
}
