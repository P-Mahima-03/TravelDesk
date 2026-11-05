package com.traveldesk.backend.service.auth;

import com.traveldesk.backend.dto.auth.RegisterRequest;
import com.traveldesk.backend.entity.Role;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.RoleRepository;
import com.traveldesk.backend.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldRegisterCustomerSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "Mahima",
                "Pilli",
                "mahima@example.com",
                "Password123",
                "9876543210"
        );

        Role customerRole = new Role();
        customerRole.setName(RoleName.CUSTOMER);

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(roleRepository.findByName(RoleName.CUSTOMER))
                .thenReturn(Optional.of(customerRole));

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        var response = registrationService.register(request);

        assertEquals("mahima@example.com", response.email());
        assertEquals("CUSTOMER", response.role());
        assertTrue(response.active());

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("Password123");
    }

    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "Mahima",
                "Pilli",
                "mahima@example.com",
                "Password123",
                "9876543210"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldFailWhenCustomerRoleDoesNotExist() {

        RegisterRequest request = new RegisterRequest(
                "Mahima",
                "Pilli",
                "mahima@example.com",
                "Password123",
                "9876543210"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(roleRepository.findByName(RoleName.CUSTOMER))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }
}