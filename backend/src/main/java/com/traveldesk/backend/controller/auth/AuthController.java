package com.traveldesk.backend.controller.auth;

import com.traveldesk.backend.dto.auth.AuthResponse;
import com.traveldesk.backend.dto.auth.LoginRequest;
import com.traveldesk.backend.dto.auth.RegisterRequest;
import com.traveldesk.backend.dto.auth.UserResponse;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.service.auth.LoginService;
import com.traveldesk.backend.service.auth.RegistrationService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = loginService.authenticate(request);

        AuthResponse response = new AuthResponse(
                "Login successful",
                user.getEmail(),
                user.getRole().getName().name(),
                null
        );

        return ResponseEntity.ok(response);
    }
}