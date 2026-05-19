package org.wimukthi.malpalathurubackend.dto;

import org.wimukthi.malpalathurubackend.enums.Language;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
        @NotBlank(message = "Player name is required")
        String playerName,

        @NotNull(message = "Language is required")
        Language language,

        @NotNull(message = "Maximum players is required")
        @Min(value = 2, message = "Minimum players should be 2")
        @Max(value = 10, message = "Maximum players should be 10")
        Integer maxPlayers,

        @NotNull(message = "Count limit is required")
        @Min(value = 10, message = "Minimum count limit should be 10 seconds")
        @Max(value = 120, message = "Maximum count limit should be 120 seconds")
        Integer countLimitSeconds,

        @NotNull(message = "Private room value is required")
        Boolean privateRoom
) {
}