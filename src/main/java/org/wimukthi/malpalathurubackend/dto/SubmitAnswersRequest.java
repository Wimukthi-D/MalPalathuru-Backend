package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswersRequest(
        @NotNull(message = "Player id is required")
        Long playerId,
        String female,
        String male,
        String flower,
        String fruit,
        String animal,
        String city
) {
}
