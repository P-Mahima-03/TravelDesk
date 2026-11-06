package com.traveldesk.backend.controller;

import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.repository.DestinationRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravelPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    @Test
    void shouldListPackagesPublicly() throws Exception {
        Destination destination = saveDestination("Kyoto");
        savePackage(destination, "Temple Walk", true);
        savePackage(destination, "Inactive Walk", false);

        mockMvc.perform(get("/api/packages").param("title", "Temple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Temple Walk"))
                .andExpect(jsonPath("$[0].destinationName").value("Kyoto"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void shouldExcludePackagesWithInactiveDestinationsFromListing() throws Exception {
        Destination inactiveDestination = saveDestination("Nara");
        inactiveDestination.setIsActive(false);
        destinationRepository.save(inactiveDestination);
        savePackage(inactiveDestination, "Inactive destination package", true);

        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldFilterPackagesByDestinationPriceAndDuration() throws Exception {
        Destination kyoto = saveDestination("Kyoto");
        Destination osaka = saveDestination("Osaka");
        savePackage(kyoto, "Kyoto temple walk", true);

        TravelPackage highPrice = savePackage(kyoto, "Kyoto luxury tour", true);
        highPrice.setBasePrice(new BigDecimal("1200.00"));
        highPrice.setDurationDays(10);
        travelPackageRepository.save(highPrice);

        savePackage(osaka, "Osaka walking tour", true);

        mockMvc.perform(get("/api/packages")
                        .param("destinationId", kyoto.getId().toString())
                        .param("minPrice", "100")
                        .param("maxPrice", "500")
                        .param("maxDurationDays", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Kyoto temple walk"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void shouldReturnPackageDetailsAndNotFoundForMissingPackage() throws Exception {
        Destination destination = saveDestination("Kyoto");
        TravelPackage travelPackage = savePackage(destination, "Temple Walk", true);

        mockMvc.perform(get("/api/packages/{id}", travelPackage.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Temple Walk"));

        mockMvc.perform(get("/api/packages/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Package not found: 999"));
    }

    @Test
    void shouldReturnNotFoundForPackageWithInactiveDestination() throws Exception {
        Destination destination = saveDestination("Nara");
        TravelPackage travelPackage = savePackage(destination, "Nara temples", true);
        destination.setIsActive(false);
        destinationRepository.save(destination);

        mockMvc.perform(get("/api/packages/{id}", travelPackage.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Package not found: " + travelPackage.getId()));
    }

    @Test
    void shouldCreatePackageForAdminWithLocationHeader() throws Exception {
        Destination destination = saveDestination("Kyoto");

        mockMvc.perform(post("/api/packages")
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packageJson(destination.getId(), "Temple Walk")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/packages/")))
                .andExpect(jsonPath("$.destinationId").value(destination.getId()))
                .andExpect(jsonPath("$.title").value("Temple Walk"));
    }

    @Test
    void shouldUpdatePackageForTravelAgent() throws Exception {
        Destination destination = saveDestination("Kyoto");
        TravelPackage travelPackage = savePackage(destination, "Old title", true);

        mockMvc.perform(put("/api/packages/{id}", travelPackage.getId())
                        .with(user(authenticatedUser("ROLE_TRAVEL_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(packageJson(destination.getId(), "New title")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void shouldSoftDeletePackageForAdmin() throws Exception {
        Destination destination = saveDestination("Kyoto");
        TravelPackage travelPackage = savePackage(destination, "Temple Walk", true);

        mockMvc.perform(delete("/api/packages/{id}", travelPackage.getId())
                        .with(user(authenticatedUser("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/packages/{id}", travelPackage.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidPackageRequest() throws Exception {
        mockMvc.perform(post("/api/packages")
                        .with(user(authenticatedUser("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destinationId": 0,
                                  "title": " ",
                                  "durationDays": 0,
                                  "basePrice": -1,
                                  "maxCapacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void shouldRequireManagementRoleForWriteOperations() throws Exception {
        Destination destination = saveDestination("Kyoto");
        TravelPackage travelPackage = savePackage(destination, "Temple Walk", true);
        String request = packageJson(destination.getId(), "Temple Walk");

        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/packages")
                        .with(user(authenticatedUser("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/packages/{id}", travelPackage.getId())
                        .with(user(authenticatedUser("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/packages/{id}", travelPackage.getId())
                        .with(user(authenticatedUser("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidPriceRange() throws Exception {
        mockMvc.perform(get("/api/packages")
                        .param("minPrice", "500")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Minimum price must not exceed maximum price"));
    }

    private Destination saveDestination(String name) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setCity(name);
        destination.setCountry("Japan");
        destination.setIsActive(true);
        return destinationRepository.save(destination);
    }

    private TravelPackage savePackage(
            Destination destination,
            String title,
            boolean active) {
        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setDestination(destination);
        travelPackage.setTitle(title);
        travelPackage.setDescription("Package description");
        travelPackage.setDurationDays(5);
        travelPackage.setBasePrice(new BigDecimal("250.00"));
        travelPackage.setMaxCapacity(10);
        travelPackage.setIsActive(active);
        return travelPackageRepository.save(travelPackage);
    }

    private UserDetails authenticatedUser(String roleName) {
        return User.withUsername("package-user@example.com")
                .password("unused")
                .authorities(roleName)
                .build();
    }

    private String packageJson(Long destinationId, String title) {
        return """
                {
                  "destinationId": %d,
                  "title": "%s",
                  "description": "A guided tour",
                  "durationDays": 5,
                  "basePrice": 250.00,
                  "maxCapacity": 10
                }
                """.formatted(destinationId, title);
    }
}
