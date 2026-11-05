package com.traveldesk.backend.service.auth;

import com.traveldesk.backend.dto.auth.LoginRequest;
import com.traveldesk.backend.dto.auth.AuthResponse;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.repository.UserRepository;
import com.traveldesk.backend.security.JwtService;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = authenticate(request);
        return new AuthResponse(
                "Login successful",
                user.getEmail(),
                user.getRole().getName().name(),
                jwtService.generateToken(user)
        );
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Account is inactive");
        }

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new BadCredentialsException(
                    "Invalid email or password");
        }

        return user;
    }
}