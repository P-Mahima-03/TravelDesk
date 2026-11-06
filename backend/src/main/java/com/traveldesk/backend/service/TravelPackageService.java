package com.traveldesk.backend.service;

import com.traveldesk.backend.dto.travelpackage.PackageRequest;
import com.traveldesk.backend.dto.travelpackage.PackageResponse;
import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.repository.DestinationRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TravelPackageService {

    private final TravelPackageRepository travelPackageRepository;
    private final DestinationRepository destinationRepository;

    public TravelPackageService(
            TravelPackageRepository travelPackageRepository,
            DestinationRepository destinationRepository) {
        this.travelPackageRepository = travelPackageRepository;
        this.destinationRepository = destinationRepository;
    }

    public List<PackageResponse> listPackages(
            String title,
            Long destinationId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer maxDurationDays) {
        validateFilters(destinationId, minPrice, maxPrice, maxDurationDays);

        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : null;
        return travelPackageRepository.findActiveByFilters(
                        normalizedTitle,
                        destinationId,
                        minPrice,
                        maxPrice,
                        maxDurationDays
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PackageResponse getPackage(Long id) {
        TravelPackage travelPackage = travelPackageRepository
                .findByIdAndIsActiveTrueAndDestination_IsActiveTrue(id)
                .orElseThrow(() -> packageNotFound(id));
        return toResponse(travelPackage);
    }

    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        Destination destination = findActiveDestination(request.destinationId());

        TravelPackage travelPackage = new TravelPackage();
        applyRequest(travelPackage, request, destination);
        travelPackage.setIsActive(true);
        LocalDateTime now = LocalDateTime.now();
        travelPackage.setCreatedAt(now);
        travelPackage.setUpdatedAt(now);

        return toResponse(travelPackageRepository.save(travelPackage));
    }

    @Transactional
    public PackageResponse updatePackage(Long id, PackageRequest request) {
        TravelPackage travelPackage = travelPackageRepository
                .findByIdAndIsActiveTrueAndDestination_IsActiveTrue(id)
                .orElseThrow(() -> packageNotFound(id));
        Destination destination = findActiveDestination(request.destinationId());

        applyRequest(travelPackage, request, destination);
        travelPackage.setUpdatedAt(LocalDateTime.now());

        return toResponse(travelPackageRepository.save(travelPackage));
    }

    @Transactional
    public void deletePackage(Long id) {
        TravelPackage travelPackage = travelPackageRepository
                .findByIdAndIsActiveTrueAndDestination_IsActiveTrue(id)
                .orElseThrow(() -> packageNotFound(id));

        travelPackage.setIsActive(false);
        travelPackage.setUpdatedAt(LocalDateTime.now());
        travelPackageRepository.save(travelPackage);
    }

    private void validateFilters(
            Long destinationId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer maxDurationDays) {
        if (destinationId != null && destinationId <= 0) {
            throw badRequest("Destination ID must be positive");
        }
        if (minPrice != null && minPrice.signum() < 0) {
            throw badRequest("Minimum price must not be negative");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw badRequest("Maximum price must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw badRequest("Minimum price must not exceed maximum price");
        }
        if (maxDurationDays != null && maxDurationDays <= 0) {
            throw badRequest("Maximum duration must be positive");
        }
    }

    private Destination findActiveDestination(Long destinationId) {
        return destinationRepository.findById(destinationId)
                .filter(destination -> Boolean.TRUE.equals(destination.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active destination not found"
                ));
    }

    private void applyRequest(
            TravelPackage travelPackage,
            PackageRequest request,
            Destination destination) {
        travelPackage.setDestination(destination);
        travelPackage.setTitle(request.title().trim());
        travelPackage.setDescription(
                StringUtils.hasText(request.description())
                        ? request.description().trim()
                        : null
        );
        travelPackage.setDurationDays(request.durationDays());
        travelPackage.setBasePrice(request.basePrice());
        travelPackage.setMaxCapacity(request.maxCapacity());
    }

    private PackageResponse toResponse(TravelPackage travelPackage) {
        return new PackageResponse(
                travelPackage.getId(),
                travelPackage.getDestination().getId(),
                travelPackage.getDestination().getName(),
                travelPackage.getTitle(),
                travelPackage.getDescription(),
                travelPackage.getDurationDays(),
                travelPackage.getBasePrice(),
                travelPackage.getMaxCapacity(),
                Boolean.TRUE.equals(travelPackage.getIsActive()),
                travelPackage.getCreatedAt(),
                travelPackage.getUpdatedAt()
        );
    }

    private ResponseStatusException packageNotFound(Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Package not found: " + id
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
