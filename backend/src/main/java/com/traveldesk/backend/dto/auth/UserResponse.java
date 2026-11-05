package com.traveldesk.backend.dto.auth;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        boolean active
) {
}