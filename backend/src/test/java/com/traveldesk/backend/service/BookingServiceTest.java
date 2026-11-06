package com.traveldesk.backend.service;

import com.traveldesk.backend.dto.booking.BookingRequest;
import com.traveldesk.backend.dto.booking.BookingResponse;
import com.traveldesk.backend.entity.Booking;
import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.entity.Role;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.BookingStatus;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.BookingRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import com.traveldesk.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelPackageRepository travelPackageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private TravelPackage travelPackage;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName(RoleName.CUSTOMER);
        customer = new User();
        customer.setId(12L);
        customer.setFirstName("Alex");
        customer.setLastName("Traveler");
        customer.setEmail("alex@example.com");
        customer.setRole(role);
        customer.setIsActive(true);

        Destination destination = new Destination();
        destination.setId(4L);
        destination.setName("Kyoto");
        destination.setIsActive(true);

        travelPackage = new TravelPackage();
        travelPackage.setId(31L);
        travelPackage.setDestination(destination);
        travelPackage.setTitle("Temple Walk");
        travelPackage.setBasePrice(new BigDecimal("125.50"));
        travelPackage.setMaxCapacity(8);
        travelPackage.setIsActive(true);
    }

    @Test
    void createsBookingWithAuthenticatedCustomerReferenceAndCalculatedTotal() {
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(31L))
                .thenReturn(Optional.of(travelPackage));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking saved = invocation.getArgument(0);
                    saved.setId(9L);
                    return saved;
                });

        BookingResponse response = bookingService.createBooking(
                customer.getEmail(),
                request(2)
        );

        assertEquals(9L, response.id());
        assertEquals(12L, response.customerId());
        assertEquals(new BigDecimal("251.00"), response.totalAmount());
        assertEquals(BookingStatus.PENDING, response.status());
        assertTrue(response.bookingReference().matches("TD-[A-F0-9]{32}"));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void rejectsMissingOrInactivePackage() {
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(31L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(customer.getEmail(), request(2))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsTravellerCountAbovePackageCapacity() {
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(31L))
                .thenReturn(Optional.of(travelPackage));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(customer.getEmail(), request(9))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectsNonFutureTravelDate() {
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        customer.getEmail(),
                        new BookingRequest(31L, LocalDate.now(), 1)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void limitsCustomerBookingListsAndReturnsNotFoundForAnotherCustomersBooking() {
        Booking booking = booking(1L, customer);
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(12L))
                .thenReturn(List.of(booking));

        assertEquals(1, bookingService.listCustomerBookings(customer.getEmail()).size());

        User otherCustomer = new User();
        otherCustomer.setId(13L);
        otherCustomer.setEmail("other@example.com");
        otherCustomer.setRole(customer.getRole());
        otherCustomer.setIsActive(true);
        when(userRepository.findByEmail(otherCustomer.getEmail()))
                .thenReturn(Optional.of(otherCustomer));
        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBooking(1L, otherCustomer.getEmail(), false)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void allowsCustomerToCancelPendingBookingAndRejectsAlreadyCancelledBooking() {
        Booking booking = booking(1L, customer);
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));
        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse cancelled =
                bookingService.cancelCustomerBooking(1L, customer.getEmail());

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertNotNull(cancelled.updatedAt());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelCustomerBooking(1L, customer.getEmail())
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void allowsManagerStatusTransitionAndRejectsUnsupportedOrInvalidTransitions() {
        Booking booking = booking(1L, customer);
        when(bookingRepository.findWithDetailsById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse confirmed =
                bookingService.updateBookingStatus(1L, BookingStatus.CONFIRMED);
        assertEquals(BookingStatus.CONFIRMED, confirmed.status());

        ResponseStatusException transitionException = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(1L, BookingStatus.PENDING)
        );
        assertEquals(HttpStatus.CONFLICT, transitionException.getStatusCode());

        ResponseStatusException completedException = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(1L, BookingStatus.COMPLETED)
        );
        assertEquals(HttpStatus.BAD_REQUEST, completedException.getStatusCode());
    }

    @Test
    void rejectsInactiveNonCustomerAccount() {
        customer.setIsActive(false);
        when(userRepository.findByEmail(customer.getEmail()))
                .thenReturn(Optional.of(customer));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.listCustomerBookings(customer.getEmail())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertFalse(customer.getIsActive());
    }

    private BookingRequest request(int travellerCount) {
        return new BookingRequest(31L, LocalDate.now().plusDays(30), travellerCount);
    }

    private Booking booking(Long id, User owner) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setBookingReference("TD-0123456789ABCDEF0123456789ABCDEF");
        booking.setCustomer(owner);
        booking.setTravelPackage(travelPackage);
        booking.setTravelDate(LocalDate.now().plusDays(30));
        booking.setNumberOfTravellers(1);
        booking.setTotalAmount(travelPackage.getBasePrice());
        booking.setBookingStatus(BookingStatus.PENDING);
        return booking;
    }
}
