package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotNull;
import org.wimukthi.malpalathurubackend.enums.ReviewDecision;

public record ReviewDecisionRequest(
        @NotNull(message = "Answer id is required")
        Long answerId,

        @NotNull(message = "Decision is required")
        ReviewDecision decision
) {
}
