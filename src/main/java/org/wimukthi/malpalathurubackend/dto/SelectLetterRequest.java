package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SelectLetterRequest(
        @NotNull(message = "Player id is required")
        Long playerId,

        @NotBlank(message = "Letter is required")
        String letter
) {
}
