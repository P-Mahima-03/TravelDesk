package com.traveldesk.backend.controller;

import com.traveldesk.backend.dto.travelpackage.PackageRequest;
import com.traveldesk.backend.dto.travelpackage.PackageResponse;
import com.traveldesk.backend.service.TravelPackageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class TravelPackageController {

    private final TravelPackageService travelPackageService;

    public TravelPackageController(TravelPackageService travelPackageService) {
        this.travelPackageService = travelPackageService;
    }

    @GetMapping
    public ResponseEntity<List<PackageResponse>> listPackages(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer maxDurationDays) {
        return ResponseEntity.ok(travelPackageService.listPackages(
                title,
                destinationId,
                minPrice,
                maxPrice,
                maxDurationDays
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackage(@PathVariable Long id) {
        return ResponseEntity.ok(travelPackageService.getPackage(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody PackageRequest request) {
        PackageResponse response = travelPackageService.createPackage(request);
        return ResponseEntity
                .created(URI.create("/api/packages/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageRequest request) {
        return ResponseEntity.ok(travelPackageService.updatePackage(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAVEL_AGENT')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        travelPackageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
