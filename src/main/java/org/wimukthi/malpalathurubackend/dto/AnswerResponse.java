package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.AnswerCategory;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        PlayerResponse player,
        AnswerCategory category,
        String answerText,
        Boolean autoSubmitted,
        LocalDateTime submittedAt,
        ReviewResponse review,
        Integer points
) {
}
