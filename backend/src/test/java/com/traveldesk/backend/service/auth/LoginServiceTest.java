package com.traveldesk.backend.service.auth;

import com.traveldesk.backend.dto.auth.LoginRequest;
import com.traveldesk.backend.entity.Role;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.UserRepository;
import com.traveldesk.backend.dto.auth.AuthResponse;
import com.traveldesk.backend.security.JwtService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String TEST_JWT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginService loginService;

    private User createUser(boolean active) {
        Role role = new Role();
        role.setName(RoleName.CUSTOMER);

        User user = new User();
        user.setId(1L);
        user.setEmail("mahima@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsActive(active);

        return user;
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        LoginRequest request =
                new LoginRequest("mahima@example.com", "Password123");

        User user = createUser(true);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                request.password(), user.getPassword()))
                .thenReturn(true);

        User result = loginService.authenticate(request);

        assertEquals("mahima@example.com", result.getEmail());
        verify(passwordEncoder).matches(
                "Password123", "encoded-password");
    }

    @Test
    void shouldReturnJwtAfterSuccessfulLogin() {
        LoginRequest request =
                new LoginRequest("mahima@example.com", "Password123");
        User user = createUser(true);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                request.password(), user.getPassword()))
                .thenReturn(true);
        JwtService realJwtService =
                new JwtService(TEST_JWT_SECRET, 3_600_000);
        LoginService loginServiceWithJwt = new LoginService(
                userRepository,
                passwordEncoder,
                realJwtService
        );

        AuthResponse response = loginServiceWithJwt.login(request);

        assertEquals("mahima@example.com", response.email());
        assertEquals("CUSTOMER", response.role());
        assertTrue(realJwtService.isTokenValid(response.token(), user));
        assertEquals(
                "mahima@example.com",
                realJwtService.extractUsername(response.token())
        );
    }

    @Test
    void shouldRejectUnknownEmail() {
        LoginRequest request =
                new LoginRequest("unknown@example.com", "Password123");

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> loginService.authenticate(request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldRejectIncorrectPassword() {
        LoginRequest request =
                new LoginRequest("mahima@example.com", "WrongPassword");

        User user = createUser(true);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                request.password(), user.getPassword()))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> loginService.authenticate(request));
    }

    @Test
    void shouldRejectInactiveAccount() {
        LoginRequest request =
                new LoginRequest("mahima@example.com", "Password123");

        User user = createUser(false);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> loginService.authenticate(request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}