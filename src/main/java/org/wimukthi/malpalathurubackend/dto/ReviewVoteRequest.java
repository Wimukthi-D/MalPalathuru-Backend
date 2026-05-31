package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotNull;
import org.wimukthi.malpalathurubackend.enums.VoteValue;

public record ReviewVoteRequest(
        @NotNull(message = "Player id is required")
        Long playerId,

        @NotNull(message = "Answer id is required")
        Long answerId,

        @NotNull(message = "Vote value is required")
        VoteValue vote
) {
}
