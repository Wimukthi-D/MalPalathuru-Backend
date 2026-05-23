package org.wimukthi.malpalathurubackend.dto;

public record PlayerResponse(
        Long id,
        String playerName,
        Boolean host,
        Boolean connected,
        Integer totalScore
) {
}
