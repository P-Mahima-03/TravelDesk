package com.traveldesk.backend.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record BookingRequest(
        @NotNull(message = "Package ID is required")
        @Positive(message = "Package ID must be positive")
        Long packageId,

        @NotNull(message = "Travel date is required")
        @Future(message = "Travel date must be in the future")
        LocalDate travelDate,

        @NotNull(message = "Number of travellers is required")
        @Positive(message = "Number of travellers must be positive")
        Integer numberOfTravellers
) {
}
