package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.AnswerCategory;
import org.wimukthi.malpalathurubackend.enums.ReviewDecision;

import java.util.List;

public record ReviewItemResponse(
        Long answerId,
        AnswerCategory category,
        PlayerResponse player,
        String answerText,
        ReviewDecision decision,
        Long acceptVotes,
        Long rejectVotes,
        List<VoteResponse> votes
) {
}
