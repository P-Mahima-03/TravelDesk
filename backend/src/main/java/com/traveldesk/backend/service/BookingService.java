package com.traveldesk.backend.service;

import com.traveldesk.backend.dto.booking.BookingRequest;
import com.traveldesk.backend.dto.booking.BookingResponse;
import com.traveldesk.backend.entity.Booking;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.BookingStatus;
import com.traveldesk.backend.enums.RoleName;
import com.traveldesk.backend.repository.BookingRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import com.traveldesk.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final BigDecimal MAX_BOOKING_AMOUNT =
            new BigDecimal("9999999999.99");

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository travelPackageRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            TravelPackageRepository travelPackageRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.travelPackageRepository = travelPackageRepository;
        this.userRepository = userRepository;
    }

    public List<BookingResponse> listCustomerBookings(String email) {
        User customer = findCustomer(email);
        return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookingResponse> listAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookingResponse getBooking(Long id, String email, boolean bookingManager) {
        Booking booking = findBooking(id);
        if (!bookingManager) {
            User customer = findCustomer(email);
            if (!booking.getCustomer().getId().equals(customer.getId())) {
                throw bookingNotFound(id);
            }
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {
        User customer = findCustomer(email);
        if (request.travelDate() == null || !request.travelDate().isAfter(LocalDate.now())) {
            throw badRequest("Travel date must be in the future");
        }
        if (request.numberOfTravellers() == null || request.numberOfTravellers() < 1) {
            throw badRequest("Number of travellers must be positive");
        }

        TravelPackage travelPackage = travelPackageRepository
                .findByIdAndIsActiveTrueAndDestination_IsActiveTrue(request.packageId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active package not found"
                ));
        if (travelPackage.getMaxCapacity() != null
                && request.numberOfTravellers() > travelPackage.getMaxCapacity()) {
            throw badRequest("Number of travellers exceeds package capacity");
        }

        BigDecimal totalAmount = travelPackage.getBasePrice()
                .multiply(BigDecimal.valueOf(request.numberOfTravellers()));
        if (totalAmount.compareTo(MAX_BOOKING_AMOUNT) > 0) {
            throw badRequest("Total booking amount exceeds the supported limit");
        }

        Booking booking = new Booking();
        booking.setBookingReference("TD-" + UUID.randomUUID().toString()
                .replace("-", "").toUpperCase(Locale.ROOT));
        booking.setCustomer(customer);
        booking.setTravelPackage(travelPackage);
        booking.setTravelDate(request.travelDate());
        booking.setNumberOfTravellers(request.numberOfTravellers());
        booking.setTotalAmount(totalAmount);
        booking.setBookingStatus(BookingStatus.PENDING);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelCustomerBooking(Long id, String email) {
        User customer = findCustomer(email);
        Booking booking = findBooking(id);
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw bookingNotFound(id);
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking cannot be cancelled in its current status"
            );
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, BookingStatus status) {
        if (status == null || status == BookingStatus.COMPLETED) {
            throw badRequest("Supported booking statuses are PENDING, CONFIRMED, and CANCELLED");
        }

        Booking booking = findBooking(id);
        BookingStatus currentStatus = booking.getBookingStatus();
        boolean validTransition = switch (currentStatus) {
            case PENDING -> status == BookingStatus.CONFIRMED
                    || status == BookingStatus.CANCELLED;
            case CONFIRMED -> status == BookingStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking status cannot be changed from " + currentStatus + " to " + status
            );
        }

        booking.setBookingStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    private User findCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer profile not found"
                ));
        if (user.getRole().getName() != RoleName.CUSTOMER
                || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An active customer account is required"
            );
        }
        return user;
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> bookingNotFound(id));
    }

    private BookingResponse toResponse(Booking booking) {
        User customer = booking.getCustomer();
        TravelPackage travelPackage = booking.getTravelPackage();
        String customerName = customer.getLastName() == null
                || customer.getLastName().isBlank()
                ? customer.getFirstName()
                : customer.getFirstName() + " " + customer.getLastName().trim();
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                customer.getId(),
                customerName,
                travelPackage.getId(),
                travelPackage.getTitle(),
                travelPackage.getDestination().getName(),
                booking.getTravelDate(),
                booking.getNumberOfTravellers(),
                booking.getTotalAmount(),
                booking.getBookingStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private ResponseStatusException bookingNotFound(Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Booking not found: " + id
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
