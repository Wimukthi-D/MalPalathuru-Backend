package org.wimukthi.malpalathurubackend.dto;

import java.time.LocalDateTime;

public record TimerResponse(
        String roomCode,
        Long roundId,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        Integer countLimitSeconds
) {
}
