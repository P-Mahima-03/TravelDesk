
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.Booking;
import com.traveldesk.backend.enums.BookingStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String reference);
    List<Booking> findByCustomer_Id(Long customerId);
    List<Booking> findByBookingStatus(BookingStatus status);
    boolean existsByBookingReference(String reference);

    @EntityGraph(attributePaths = {"customer", "travelPackage", "travelPackage.destination"})
    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "travelPackage", "travelPackage.destination"})
    List<Booking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "travelPackage", "travelPackage.destination"})
    Optional<Booking> findWithDetailsById(Long id);
}
