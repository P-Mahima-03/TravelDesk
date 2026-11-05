
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String reference);
    List<Booking> findByCustomer_Id(Long customerId);
    List<Booking> findByBookingStatus(String status);
    boolean existsByBookingReference(String reference);
}
