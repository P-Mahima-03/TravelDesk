package com.traveldesk.backend.dto.travelpackage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PackageResponse(
        Long id,
        Long destinationId,
        String destinationName,
        String title,
        String description,
        Integer durationDays,
        BigDecimal basePrice,
        Integer maxCapacity,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
