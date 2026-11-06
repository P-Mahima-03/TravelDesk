package com.traveldesk.backend.dto.booking;

import com.traveldesk.backend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record BookingStatusRequest(
        @NotNull(message = "Booking status is required")
        BookingStatus status
) {
}
