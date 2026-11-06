package com.traveldesk.backend.service;

import com.traveldesk.backend.dto.destination.DestinationRequest;
import com.traveldesk.backend.dto.destination.DestinationResponse;
import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.repository.DestinationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DestinationService {

    private final DestinationRepository destinationRepository;

    public DestinationService(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    public List<DestinationResponse> listDestinations(String name, String city, String country) {
        List<Destination> destinations = destinationRepository.findByIsActiveTrueOrderByNameAsc();

        String trimmedName = normalizeFilter(name);
        String trimmedCity = normalizeFilter(city);
        String trimmedCountry = normalizeFilter(country);

        return destinations.stream()
                .filter(destination -> matchesFilter(destination, trimmedName, trimmedCity, trimmedCountry))
                .sorted(Comparator.comparing(Destination::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toResponse)
                .toList();
    }

    public DestinationResponse getDestination(Long id) {
        Destination destination = destinationRepository.findById(id)
                .filter(Destination::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination not found"
                ));

        return toResponse(destination);
    }

    @Transactional
    public DestinationResponse createDestination(DestinationRequest request) {
        String normalizedName = normalizeText(request.name());
        String normalizedCity = normalizeText(request.city());
        String normalizedCountry = normalizeText(request.country());
        String normalizedDescription = normalizeOptionalText(request.description());
        String normalizedImageUrl = normalizeOptionalText(request.imageUrl());

        if (destinationRepository.existsByNameIgnoreCaseAndCountryIgnoreCaseAndIsActiveTrue(normalizedName, normalizedCountry)) {
            throw new IllegalArgumentException("A destination with this name already exists in the specified country");
        }

        Destination destination = new Destination();
        destination.setName(normalizedName);
        destination.setCity(normalizedCity);
        destination.setCountry(normalizedCountry);
        destination.setDescription(normalizedDescription);
        destination.setImageUrl(normalizedImageUrl);
        destination.setIsActive(true);
        destination.setCreatedAt(LocalDateTime.now());
        destination.setUpdatedAt(LocalDateTime.now());

        return toResponse(destinationRepository.save(destination));
    }

    @Transactional
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        Destination destination = destinationRepository.findById(id)
                .filter(Destination::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination not found"
                ));

        String normalizedName = normalizeText(request.name());
        String normalizedCity = normalizeText(request.city());
        String normalizedCountry = normalizeText(request.country());
        String normalizedDescription = normalizeOptionalText(request.description());
        String normalizedImageUrl = normalizeOptionalText(request.imageUrl());

        boolean duplicateNameCountry = destinationRepository
                .existsByNameIgnoreCaseAndCountryIgnoreCaseAndIsActiveTrueAndIdNot(
                        normalizedName,
                        normalizedCountry,
                        id
                );

        if (duplicateNameCountry) {
            throw new IllegalArgumentException("A destination with this name already exists in the specified country");
        }

        destination.setName(normalizedName);
        destination.setCity(normalizedCity);
        destination.setCountry(normalizedCountry);
        destination.setDescription(normalizedDescription);
        destination.setImageUrl(normalizedImageUrl);
        destination.setUpdatedAt(LocalDateTime.now());

        return toResponse(destinationRepository.save(destination));
    }

    @Transactional
    public void deleteDestination(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination not found"
                ));

        if (!Boolean.TRUE.equals(destination.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found");
        }

        destination.setIsActive(false);
        destination.setUpdatedAt(LocalDateTime.now());
        destinationRepository.save(destination);
    }

    private boolean matchesFilter(
            Destination destination,
            String name,
            String city,
            String country) {

        if (StringUtils.hasText(name)
                && (destination.getName() == null
                        || !destination.getName().toLowerCase().contains(name.toLowerCase()))) {
            return false;
        }

        if (StringUtils.hasText(city)
                && (destination.getCity() == null
                        || !destination.getCity().toLowerCase().contains(city.toLowerCase()))) {
            return false;
        }

        if (StringUtils.hasText(country)
                && (destination.getCountry() == null
                        || !destination.getCountry().toLowerCase().contains(country.toLowerCase()))) {
            return false;
        }

        return true;
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private DestinationResponse toResponse(Destination destination) {
        return new DestinationResponse(
                destination.getId(),
                destination.getName(),
                destination.getCity(),
                destination.getCountry(),
                destination.getDescription(),
                destination.getImageUrl(),
                Boolean.TRUE.equals(destination.getIsActive()),
                destination.getCreatedAt(),
                destination.getUpdatedAt()
        );
    }
}
