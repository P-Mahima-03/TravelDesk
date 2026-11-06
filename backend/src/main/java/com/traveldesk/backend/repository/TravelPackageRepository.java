
package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.TravelPackage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {
    List<TravelPackage> findByDestination_Id(Long destinationId);
    List<TravelPackage> findByIsActiveTrue();
    List<TravelPackage> findByBasePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<TravelPackage> findByDurationDaysLessThanEqual(Integer days);

    @EntityGraph(attributePaths = "destination")
    Optional<TravelPackage> findByIdAndIsActiveTrueAndDestination_IsActiveTrue(Long id);

    @EntityGraph(attributePaths = "destination")
    @Query("""
            SELECT travelPackage
            FROM TravelPackage travelPackage
            WHERE travelPackage.isActive = true
              AND travelPackage.destination.isActive = true
              AND (:title IS NULL OR LOWER(travelPackage.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:destinationId IS NULL OR travelPackage.destination.id = :destinationId)
              AND (:minPrice IS NULL OR travelPackage.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR travelPackage.basePrice <= :maxPrice)
              AND (:maxDurationDays IS NULL OR travelPackage.durationDays <= :maxDurationDays)
            ORDER BY travelPackage.title ASC
            """)
    List<TravelPackage> findActiveByFilters(
            @Param("title") String title,
            @Param("destinationId") Long destinationId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("maxDurationDays") Integer maxDurationDays);
}
