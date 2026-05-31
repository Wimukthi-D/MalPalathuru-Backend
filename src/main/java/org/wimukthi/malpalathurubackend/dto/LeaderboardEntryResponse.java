package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.AnswerCategory;

import java.util.Map;

public record LeaderboardEntryResponse(
        PlayerResponse player,
        Integer roundScore,
        Integer totalScore,
        Map<AnswerCategory, Integer> categoryScores
) {
}
