package com.traveldesk.backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.traveldesk.backend.entity.Role;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.RoleRepository;
import com.traveldesk.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    private static final String EMAIL = "profile@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldReturnCurrentAuthenticatedUserProfileWithoutSensitiveFields()
            throws Exception {
        saveUser(EMAIL);
        UserDetails principal =
                org.springframework.security.core.userdetails.User
                        .withUsername(EMAIL)
                        .password("unused")
                        .authorities("ROLE_CUSTOMER")
                        .build();

        mockMvc.perform(get("/api/users/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alex"))
                .andExpect(jsonPath("$.lastName").value("Traveler"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phone").value("555-0100"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserHasNoProfile()
            throws Exception {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("missing@example.com")
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();

        mockMvc.perform(get("/api/users/me").with(user(principal)))
                .andExpect(status().isNotFound());
    }

    private User saveUser(String email) {
        Role role = new Role();
        role.setName(RoleName.CUSTOMER);
        Role savedRole = roleRepository.save(role);

        User user = new User();
        user.setRole(savedRole);
        user.setFirstName("Alex");
        user.setLastName("Traveler");
        user.setEmail(email);
        user.setPassword("must-not-be-returned");
        user.setPhone("555-0100");
        user.setIsActive(true);
        return userRepository.save(user);
    }
}
