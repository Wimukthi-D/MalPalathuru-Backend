package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.ReviewDecision;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        ReviewDecision decision,
        Long decidedByHostPlayerId,
        LocalDateTime decidedAt,
        Long acceptVotes,
        Long rejectVotes,
        List<VoteResponse> votes
) {
}
