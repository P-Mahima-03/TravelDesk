package com.traveldesk.backend.service;

import com.traveldesk.backend.dto.travelpackage.PackageRequest;
import com.traveldesk.backend.dto.travelpackage.PackageResponse;
import com.traveldesk.backend.entity.Destination;
import com.traveldesk.backend.entity.TravelPackage;
import com.traveldesk.backend.repository.DestinationRepository;
import com.traveldesk.backend.repository.TravelPackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelPackageServiceTest {

    @Mock
    private TravelPackageRepository travelPackageRepository;

    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private TravelPackageService travelPackageService;

    private Destination destination;

    @BeforeEach
    void setUp() {
        destination = new Destination();
        destination.setId(8L);
        destination.setName("Kyoto");
        destination.setIsActive(true);
    }

    @Test
    void shouldListActivePackagesAndApplyAllOptionalFilters() {
        TravelPackage travelPackage = createPackage(1L, "Temple Walk");
        when(travelPackageRepository.findActiveByFilters(
                "temple",
                8L,
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                8
        )).thenReturn(List.of(travelPackage));

        List<PackageResponse> results = travelPackageService.listPackages(
                " temple ",
                8L,
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                8
        );

        assertEquals(1, results.size());
        assertEquals("Temple Walk", results.get(0).title());
        verify(travelPackageRepository).findActiveByFilters(
                "temple",
                8L,
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                8
        );
    }

    @Test
    void shouldExcludeInactivePackagesFromListing() {
        when(travelPackageRepository.findActiveByFilters(null, null, null, null, null))
                .thenReturn(List.of(createPackage(1L, "Active package")));

        List<PackageResponse> results =
                travelPackageService.listPackages(null, null, null, null, null);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isActive());
    }

    @Test
    void shouldReturnActivePackageDetails() {
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(1L))
                .thenReturn(Optional.of(createPackage(1L, "Temple Walk")));

        PackageResponse response = travelPackageService.getPackage(1L);

        assertEquals(1L, response.id());
        assertEquals(8L, response.destinationId());
        assertEquals("Kyoto", response.destinationName());
    }

    @Test
    void shouldReturnNotFoundForMissingOrInactivePackage() {
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelPackageService.getPackage(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldCreatePackageAndNormalizeText() {
        PackageRequest request = request(8L, "  Temple Walk  ", "  A guided tour  ");
        when(destinationRepository.findById(8L)).thenReturn(Optional.of(destination));
        when(travelPackageRepository.save(any(TravelPackage.class)))
                .thenAnswer(invocation -> {
                    TravelPackage saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        PackageResponse response = travelPackageService.createPackage(request);

        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(travelPackageRepository).save(captor.capture());
        assertEquals("Temple Walk", captor.getValue().getTitle());
        assertEquals("A guided tour", captor.getValue().getDescription());
        assertTrue(captor.getValue().getIsActive());
        assertNotNull(captor.getValue().getCreatedAt());
        assertEquals(2L, response.id());
        assertEquals("Kyoto", response.destinationName());
    }

    @Test
    void shouldRejectMissingOrInactiveDestination() {
        Destination inactiveDestination = new Destination();
        inactiveDestination.setIsActive(false);
        when(destinationRepository.findById(8L))
                .thenReturn(Optional.of(inactiveDestination));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelPackageService.createPackage(request(8L, "Temple Walk", null))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldRejectMissingDestination() {
        when(destinationRepository.findById(8L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelPackageService.createPackage(request(8L, "Temple Walk", null))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldRejectInvalidPriceRange() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> travelPackageService.listPackages(
                        null,
                        null,
                        new BigDecimal("500"),
                        new BigDecimal("100"),
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectNegativePriceFilterAndNonPositiveDurationFilter() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(
                        ResponseStatusException.class,
                        () -> travelPackageService.listPackages(
                                null, null, new BigDecimal("-1"), null, null
                        )
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(
                        ResponseStatusException.class,
                        () -> travelPackageService.listPackages(null, null, null, null, 0)
                ).getStatusCode()
        );
    }

    @Test
    void shouldUpdateExistingPackageAndPreserveCreationTime() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(3);
        TravelPackage existing = createPackage(3L, "Old title");
        existing.setCreatedAt(createdAt);
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(3L))
                .thenReturn(Optional.of(existing));
        when(destinationRepository.findById(8L)).thenReturn(Optional.of(destination));
        when(travelPackageRepository.save(any(TravelPackage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PackageResponse response = travelPackageService.updatePackage(
                3L,
                request(8L, "  New title ", " ")
        );

        assertEquals("New title", response.title());
        assertEquals(createdAt, response.createdAt());
        assertNotNull(response.updatedAt());
        assertTrue(response.updatedAt().isAfter(createdAt));
        assertNull(response.description());
    }

    @Test
    void shouldSoftDeletePackage() {
        TravelPackage existing = createPackage(4L, "To remove");
        when(travelPackageRepository.findByIdAndIsActiveTrueAndDestination_IsActiveTrue(4L))
                .thenReturn(Optional.of(existing));
        when(travelPackageRepository.save(any(TravelPackage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        travelPackageService.deletePackage(4L);

        assertFalse(existing.getIsActive());
        assertNotNull(existing.getUpdatedAt());
        verify(travelPackageRepository).save(existing);
    }

    private PackageRequest request(Long destinationId, String title, String description) {
        return new PackageRequest(
                destinationId,
                title,
                description,
                5,
                new BigDecimal("250.00"),
                12
        );
    }

    private TravelPackage createPackage(Long id, String title) {
        TravelPackage travelPackage = new TravelPackage();
        travelPackage.setId(id);
        travelPackage.setDestination(destination);
        travelPackage.setTitle(title);
        travelPackage.setDescription("A guided tour");
        travelPackage.setDurationDays(5);
        travelPackage.setBasePrice(new BigDecimal("250.00"));
        travelPackage.setMaxCapacity(12);
        travelPackage.setIsActive(true);
        travelPackage.setCreatedAt(LocalDateTime.now().minusDays(1));
        travelPackage.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return travelPackage;
    }
}
