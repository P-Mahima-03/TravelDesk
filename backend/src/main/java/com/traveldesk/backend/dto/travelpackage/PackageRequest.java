package com.traveldesk.backend.dto.travelpackage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PackageRequest(

        @NotNull(message = "Destination is required")
        @Positive(message = "Destination ID must be positive")
        Long destinationId,

        @NotBlank(message = "Package title is required")
        @Size(max = 200, message = "Package title must not exceed 200 characters")
        String title,

        String description,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be positive")
        Integer durationDays,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", message = "Base price must not be negative")
        BigDecimal basePrice,

        @Positive(message = "Maximum capacity must be positive")
        Integer maxCapacity
) {
}
