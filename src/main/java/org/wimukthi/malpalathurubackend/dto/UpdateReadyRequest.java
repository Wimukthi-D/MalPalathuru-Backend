package org.wimukthi.malpalathurubackend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateReadyRequest(
        @NotNull(message = "Ready value is required")
        Boolean ready
) {
}
