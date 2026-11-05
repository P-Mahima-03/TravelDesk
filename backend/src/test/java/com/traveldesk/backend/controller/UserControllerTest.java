package com.traveldesk.backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void shouldUpdateCurrentUserProfileAndReturnUpdatedSafeProfile()
            throws Exception {
        User user = saveUser(EMAIL);
        UserDetails principal = authenticatedUser(EMAIL);

        mockMvc.perform(put("/api/users/me")
                        .with(user(principal))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "  Alexia ",
                                  "lastName": " Explorer ",
                                  "phone": " 555-0199 ",
                                  "email": "attacker@example.com",
                                  "password": "changed-password",
                                  "role": "ADMIN",
                                  "active": false,
                                  "id": 999
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alexia"))
                .andExpect(jsonPath("$.lastName").value("Explorer"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.phone").value("555-0199"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                "must-not-be-returned", updatedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertEquals(EMAIL, updatedUser.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals(
                RoleName.CUSTOMER, updatedUser.getRole().getName());
        org.junit.jupiter.api.Assertions.assertTrue(updatedUser.getIsActive());
    }

    @Test
    void shouldRejectBlankNamesAndPhoneLongerThanExistingLimit()
            throws Exception {
        saveUser(EMAIL);

        mockMvc.perform(put("/api/users/me")
                        .with(user(authenticatedUser(EMAIL)))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": " ",
                                  "lastName": "",
                                  "phone": "123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").value(
                        org.hamcrest.Matchers.containsInAnyOrder(
                                "First name is required",
                                "Last name is required",
                                "Phone number must not exceed 20 characters"
                        )));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingAuthenticatedUser()
            throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .with(user(authenticatedUser("missing@example.com")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alex",
                                  "lastName": "Traveler",
                                  "phone": null
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private UserDetails authenticatedUser(String email) {
        return org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();
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
