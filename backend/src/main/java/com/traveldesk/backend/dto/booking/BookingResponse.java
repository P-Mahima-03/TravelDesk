package com.traveldesk.backend.dto.booking;

import com.traveldesk.backend.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long customerId,
        String customerName,
        Long packageId,
        String packageTitle,
        String destinationName,
        LocalDate travelDate,
        Integer numberOfTravellers,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
