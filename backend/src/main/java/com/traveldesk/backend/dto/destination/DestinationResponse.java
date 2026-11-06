package com.traveldesk.backend.dto.destination;

import java.time.LocalDateTime;

public record DestinationResponse(
        Long id,
        String name,
        String city,
        String country,
        String description,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
