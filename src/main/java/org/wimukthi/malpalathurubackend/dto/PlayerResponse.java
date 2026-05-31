package org.wimukthi.malpalathurubackend.dto;

public record PlayerResponse(
        Long id,
        String playerName,
        Boolean host,
        Boolean ready,
        Boolean connected,
        Integer totalScore
) {
    public PlayerResponse(Long id, String playerName, Boolean host, Boolean connected, Integer totalScore) {
        this(id, playerName, host, false, connected, totalScore);
    }
}
