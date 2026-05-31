package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.RoundStatus;

public record RoundSummaryResponse(
        Long id,
        Integer roundNumber,
        RoundStatus status,
        String selectedLetter,
        PlayerResponse suggester
) {
}
