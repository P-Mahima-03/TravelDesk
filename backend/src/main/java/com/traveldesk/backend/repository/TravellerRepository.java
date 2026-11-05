
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.Traveller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TravellerRepository extends JpaRepository<Traveller, Long> {
    List<Traveller> findByBooking_Id(Long bookingId);
    List<Traveller> findByPassportNumber(String passportNumber);
}
