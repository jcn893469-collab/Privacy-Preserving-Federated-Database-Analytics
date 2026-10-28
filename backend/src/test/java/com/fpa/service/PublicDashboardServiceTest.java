package com.fpa.service;

import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicDashboardServiceTest {

    @Mock
    private FederatedDatabaseRepository federatedDatabaseRepository;

    @Mock
    private AnalyticsContributionRepository analyticsContributionRepository;

    @InjectMocks
    private PublicDashboardService publicDashboardService;

    @Test
    void aggregatesPersistedProtectedSumsUsingThePersistedRecordCount() {
        User owner = User.builder()
            .email("owner@example.org")
            .build();
        FederatedDatabase database = FederatedDatabase.builder()
            .organizationName("Synthetic Organization")
            .dbIdentifier("synthetic-db")
            .owner(owner)
            .active(true)
            .registeredAt(LocalDateTime.now())
            .privacyEpsilonBudget(new BigDecimal("10.0000"))
            .privacyEpsilonSpent(new BigDecimal("1.2500"))
            .build();

        when(analyticsContributionRepository.sumGlobalRecordCount()).thenReturn(20L);
        when(analyticsContributionRepository.sumGlobalNoisySum()).thenReturn(new BigDecimal("87.6816"));
        when(analyticsContributionRepository.count()).thenReturn(3L);
        when(federatedDatabaseRepository.countDistinctOrganizationNames()).thenReturn(2L);
        when(federatedDatabaseRepository.countByActiveTrue()).thenReturn(1L);
        when(federatedDatabaseRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of(database));

        var response = publicDashboardService.getDashboard();

        assertEquals(2, response.totalOrganizations());
        assertEquals(1, response.activeOrganizations());
        assertEquals(3, response.totalContributions());
        assertEquals(20, response.totalProtectedRecords());
        assertEquals(new BigDecimal("87.6816"), response.globalProtectedSum());
        assertEquals(new BigDecimal("4.384080"), response.globalProtectedMean());
        assertEquals("owner@example.org", response.databases().get(0).ownerEmail());
        assertEquals("synthetic-db", response.databases().get(0).dbIdentifier());
    }

    @Test
    void doesNotFabricateMeanWhenThereAreNoProtectedRecords() {
        when(analyticsContributionRepository.sumGlobalRecordCount()).thenReturn(0L);
        when(analyticsContributionRepository.sumGlobalNoisySum()).thenReturn(BigDecimal.ZERO);
        when(analyticsContributionRepository.count()).thenReturn(0L);
        when(federatedDatabaseRepository.countDistinctOrganizationNames()).thenReturn(0L);
        when(federatedDatabaseRepository.countByActiveTrue()).thenReturn(0L);
        when(federatedDatabaseRepository.findAllByOrderByRegisteredAtDesc()).thenReturn(List.of());

        var response = publicDashboardService.getDashboard();

        assertEquals(0, response.totalContributions());
        assertEquals(0, response.totalProtectedRecords());
        assertEquals(BigDecimal.ZERO, response.globalProtectedSum());
        assertNull(response.globalProtectedMean());
        assertEquals(List.of(), response.databases());
    }
}
