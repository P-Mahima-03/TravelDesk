package com.traveldesk.backend.controller;

import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.repository.DestinationRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DestinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DestinationRepository destinationRepository;

    @Test
    void shouldListActiveDestinationsInNameOrder() throws Exception {
        saveDestination("Santorini", "Fira", "Greece");
        saveDestination("Bali", "Ubud", "Indonesia");

        mockMvc.perform(get("/api/destinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bali"))
                .andExpect(jsonPath("$[0].country").value("Indonesia"))
                .andExpect(jsonPath("$[1].name").value("Santorini"));
    }

    @Test
    void shouldReturnDestinationDetailsForExistingDestination() throws Exception {
        Destination destination = saveDestination("Kyiv", "Kyiv", "Ukraine");

        mockMvc.perform(get("/api/destinations/{id}", destination.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(destination.getId()))
                .andExpect(jsonPath("$.city").value("Kyiv"))
                .andExpect(jsonPath("$.country").value("Ukraine"));
    }

    @Test
    void shouldReturnNotFoundForMissingDestination() throws Exception {
        mockMvc.perform(get("/api/destinations/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Destination not found"));
    }

    @Test
    void shouldCreateDestinationForAuthorizedAdmin() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Paris",
                                  "city": "Paris",
                                  "country": "France",
                                  "description": "City of lights",
                                  "imageUrl": "https://example.com/paris.jpg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paris"))
                .andExpect(jsonPath("$.country").value("France"));
    }

    @Test
    void shouldRejectCreateDestinationForUnauthorizedCustomer() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .with(user(authenticatedUser("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rome",
                                  "city": "Rome",
                                  "country": "Italy"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicateDestination() throws Exception {
        saveDestination("Bali", "Ubud", "Indonesia");

        mockMvc.perform(post("/api/destinations")
                        .with(user(authenticatedUser("ROLE_TRAVEL_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "bali",
                                  "city": "Ubud",
                                  "country": "indonesia"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A destination with this name already exists in the specified country"));
    }

    @Test
    void shouldAllowRecreatingDestinationAfterSoftDelete() throws Exception {
        Destination removed = saveDestination("Rome", "Rome", "Italy");
        removed.setIsActive(false);
        destinationRepository.save(removed);

        mockMvc.perform(post("/api/destinations")
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rome",
                                  "city": "Rome",
                                  "country": "Italy"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rome"));
    }

    @Test
    void shouldSafelyExcludeNullCityWhenFilteringByCity() throws Exception {
        Destination destination = new Destination();
        destination.setName("Null City Place");
        destination.setCity(null);
        destination.setCountry("Italy");
        destination.setDescription("No city record");
        destination.setImageUrl("https://example.com/null-city.jpg");
        destination.setIsActive(true);
        destinationRepository.save(destination);

        mockMvc.perform(get("/api/destinations").param("city", "Rome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldRejectInvalidDestinationRequest() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "city": "",
                                  "country": "",
                                  "description": "x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void shouldUpdateDestinationForAuthorizedUser() throws Exception {
        Destination destination = saveDestination("Tokyo", "Tokyo", "Japan");

        mockMvc.perform(put("/api/destinations/{id}", destination.getId())
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tokyo Bay",
                                  "city": "Tokyo",
                                  "country": "Japan",
                                  "description": "Updated description",
                                  "imageUrl": "https://example.com/tokyo-bay.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tokyo Bay"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void shouldRejectUpdateDestinationForUnauthorizedUser() throws Exception {
        Destination destination = saveDestination("Lisbon", "Lisbon", "Portugal");

        mockMvc.perform(put("/api/destinations/{id}", destination.getId())
                        .with(user(authenticatedUser("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lisbon City",
                                  "city": "Lisbon",
                                  "country": "Portugal"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteDestinationForAuthorizedUser() throws Exception {
        Destination destination = saveDestination("Barcelona", "Barcelona", "Spain");

        mockMvc.perform(delete("/api/destinations/{id}", destination.getId())
                        .with(user(authenticatedUser("ROLE_TRAVEL_AGENT"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/destinations/{id}", destination.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDeleteDestinationForUnauthorizedUser() throws Exception {
        Destination destination = saveDestination("Dubai", "Dubai", "UAE");

        mockMvc.perform(delete("/api/destinations/{id}", destination.getId())
                        .with(user(authenticatedUser("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    private Destination saveDestination(String name, String city, String country) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setCity(city);
        destination.setCountry(country);
        destination.setDescription("Sample description");
        destination.setImageUrl("https://example.com/" + name.toLowerCase() + ".jpg");
        destination.setIsActive(true);
        return destinationRepository.save(destination);
    }

    private UserDetails authenticatedUser(String roleName) {
        return User.withUsername("user@example.com")
                .password("unused")
                .authorities(roleName)
                .build();
    }
}
