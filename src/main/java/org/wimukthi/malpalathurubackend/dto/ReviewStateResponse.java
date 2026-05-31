package org.wimukthi.malpalathurubackend.dto;

import java.util.List;

public record ReviewStateResponse(
        String roomCode,
        Long roundId,
        Integer roundNumber,
        ReviewItemResponse currentItem,
        List<ReviewItemResponse> items
) {
}
