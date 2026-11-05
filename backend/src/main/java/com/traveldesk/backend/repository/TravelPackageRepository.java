
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {
    List<TravelPackage> findByDestination_Id(Long destinationId);
    List<TravelPackage> findByIsActiveTrue();
    List<TravelPackage> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<TravelPackage> findByDurationDaysLessThanEqual(Integer days);
}
