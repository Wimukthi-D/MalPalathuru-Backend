package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.RoundStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RoundResponse(
        Long id,
        Integer roundNumber,
        RoundStatus status,
        String selectedLetter,
        PlayerResponse suggester,
        LocalDateTime countdownStartedAt,
        LocalDateTime lockedAt,
        LocalDateTime reviewStartedAt,
        LocalDateTime scoreCalculatedAt,
        List<AnswerResponse> answers
) {
}
