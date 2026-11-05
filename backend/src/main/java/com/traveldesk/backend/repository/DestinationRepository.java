
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByCountryIgnoreCase(String country);
    List<Destination> findByCityIgnoreCase(String city);
    List<Destination> findByIsActiveTrue();
    boolean existsByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);
}
