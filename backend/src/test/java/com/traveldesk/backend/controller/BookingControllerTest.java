package com.traveldesk.backend.controller;

import com.traveldesk.backend.entity.Booking;
import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.entity.Role;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.BookingStatus;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.BookingRepository;
import com.traveldesk.backend.repository.DestinationRepository;
import com.traveldesk.backend.repository.RoleRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import com.traveldesk.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

    private static final AtomicLong EMAIL_SEQUENCE = new AtomicLong();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DestinationRepository destinationRepository;

    @Autowired
    private TravelPackageRepository travelPackageRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsBookingForAuthenticatedCustomerAndIgnoresSubmittedCustomerId()
            throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        User otherCustomer = saveUser(RoleName.CUSTOMER);
        TravelPackage travelPackage = savePackage(true);

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(travelPackage.getId(), 2, otherCustomer.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith("/api/bookings/")))
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.packageId").value(travelPackage.getId()))
                .andExpect(jsonPath("$.numberOfTravellers").value(2))
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.bookingReference")
                        .value(org.hamcrest.Matchers.matchesRegex("TD-[A-F0-9]{32}")));
    }

    @Test
    void rejectsInvalidBookingFieldsAndPastTravelDate() throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        TravelPackage travelPackage = savePackage(true);

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": 0,
                                  "travelDate": "%s",
                                  "numberOfTravellers": 0
                                }
                                """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(travelPackage.getId(), 2, null)
                                .replace(LocalDate.now().plusDays(30).toString(),
                                        LocalDate.now().minusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void rejectsBookingForMissingInactivePackageOrExcessCapacity() throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        TravelPackage inactivePackage = savePackage(false);
        TravelPackage limitedPackage = savePackage(true);

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(999999L, 1, null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(inactivePackage.getId(), 1, null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(limitedPackage.getId(), 11, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Number of travellers exceeds package capacity"));
    }

    @Test
    void rejectsBookingWhenPackageDestinationIsInactive() throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        TravelPackage travelPackage = savePackage(true);
        Destination destination = travelPackage.getDestination();
        destination.setIsActive(false);
        destinationRepository.save(destination);

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(travelPackage.getId(), 1, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Active package not found"));
    }

    @Test
    void customerCanListAndReadOnlyOwnBookings() throws Exception {
        User firstCustomer = saveUser(RoleName.CUSTOMER);
        User secondCustomer = saveUser(RoleName.CUSTOMER);
        Booking ownBooking = saveBooking(firstCustomer, savePackage(true));
        Booking otherBooking = saveBooking(secondCustomer, ownBooking.getTravelPackage());

        mockMvc.perform(get("/api/bookings")
                        .with(user(authenticatedUser(firstCustomer, "ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ownBooking.getId()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/bookings/{id}", ownBooking.getId())
                        .with(user(authenticatedUser(firstCustomer, "ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value(ownBooking.getBookingReference()));

        mockMvc.perform(get("/api/bookings/{id}", otherBooking.getId())
                        .with(user(authenticatedUser(firstCustomer, "ROLE_CUSTOMER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCanCancelOwnPendingBookingButNotAnotherCustomersBooking()
            throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        User otherCustomer = saveUser(RoleName.CUSTOMER);
        TravelPackage travelPackage = savePackage(true);
        Booking ownBooking = saveBooking(customer, travelPackage);
        Booking otherBooking = saveBooking(otherCustomer, travelPackage);

        mockMvc.perform(post("/api/bookings/{id}/cancel", ownBooking.getId())
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/bookings/{id}/cancel", otherBooking.getId())
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void managersCanViewAllBookingsAndUpdateStatusButCustomersCannot()
            throws Exception {
        User customer = saveUser(RoleName.CUSTOMER);
        User anotherCustomer = saveUser(RoleName.CUSTOMER);
        User agent = saveUser(RoleName.TRAVEL_AGENT);
        Booking firstBooking = saveBooking(customer, savePackage(true));
        saveBooking(anotherCustomer, firstBooking.getTravelPackage());

        mockMvc.perform(get("/api/bookings")
                        .with(user(authenticatedUser(agent, "ROLE_TRAVEL_AGENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists())
                .andExpect(jsonPath("$[1]").exists());

        mockMvc.perform(patch("/api/bookings/{id}/status", firstBooking.getId())
                        .with(user(authenticatedUser(agent, "ROLE_TRAVEL_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/bookings/{id}/status", firstBooking.getId())
                        .with(user(authenticatedUser(customer, "ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(agent, "ROLE_TRAVEL_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(firstBooking.getTravelPackage().getId(), 1, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingEndpointsRequireAuthenticationAndCustomerRoleForCreation()
            throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized());

        User agent = saveUser(RoleName.TRAVEL_AGENT);
        TravelPackage travelPackage = savePackage(true);
        mockMvc.perform(post("/api/bookings")
                        .with(user(authenticatedUser(agent, "ROLE_TRAVEL_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(travelPackage.getId(), 1, null)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(roleName);
            return roleRepository.save(newRole);
        });
        long sequence = EMAIL_SEQUENCE.incrementAndGet();
        User user = new User();
        user.setRole(role);
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setEmail("booking-" + sequence + "@example.com");
        user.setPassword("unused");
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private TravelPackage savePackage(boolean active) {
        Destination destination = new Destination();
        destination.setName("Kyoto " + EMAIL_SEQUENCE.incrementAndGet());
        destination.setCity("Kyoto");
        destination.setCountry("Japan");
        destination.setIsActive(true);
        destination = destinationRepository.save(destination);

        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setDestination(destination);
        travelPackage.setTitle("Temple Walk " + EMAIL_SEQUENCE.incrementAndGet());
        travelPackage.setDescription("Guided visit");
        travelPackage.setDurationDays(5);
        travelPackage.setBasePrice(new BigDecimal("250.00"));
        travelPackage.setMaxCapacity(10);
        travelPackage.setIsActive(active);
        return travelPackageRepository.save(travelPackage);
    }

    private Booking saveBooking(User customer, TravelPackage travelPackage) {
        Booking booking = new Booking();
        booking.setBookingReference("TD-" + java.util.UUID.randomUUID()
                .toString().replace("-", "").toUpperCase());
        booking.setCustomer(customer);
        booking.setTravelPackage(travelPackage);
        booking.setTravelDate(LocalDate.now().plusDays(30));
        booking.setNumberOfTravellers(1);
        booking.setTotalAmount(travelPackage.getBasePrice());
        booking.setBookingStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    private UserDetails authenticatedUser(User user, String authority) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("unused")
                .authorities(authority)
                .build();
    }

    private String bookingJson(Long packageId, int travellers, Long customerId) {
        String customerField = customerId == null ? "" : """
                  "customerId": %d,
                """.formatted(customerId);
        return """
                {
                  %s
                  "packageId": %d,
                  "travelDate": "%s",
                  "numberOfTravellers": %d
                }
                """.formatted(
                customerField,
                packageId,
                LocalDate.now().plusDays(30),
                travellers
        );
    }
}
