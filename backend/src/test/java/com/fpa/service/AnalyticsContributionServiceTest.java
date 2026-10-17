package com.fpa.service;

import com.fpa.api.dto.ContributionRequest;
import com.fpa.api.dto.ContributionResponse;
import com.fpa.domain.entity.AnalyticsContribution;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.PrivacyBudgetLedger;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.PrivacyBudgetLedgerRepository;
import com.fpa.privacy.PrivacyAccountant;
import com.fpa.privacy.PrivacyBudgetExceededException;
import com.fpa.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsContributionServiceTest {

    @Mock
    private AnalyticsContributionRepository contributionRepository;

    @Mock
    private FederatedDatabaseRepository databaseRepository;

    @Mock
    private PrivacyBudgetLedgerRepository privacyBudgetLedgerRepository;

    @Mock
    private CurrentUserService currentUserService;

    private PrivacyAccountant privacyAccountant;
    private AnalyticsContributionService contributionService;

    private User owner;
    private FederatedDatabase database;

    @BeforeEach
    void setUp() {
        privacyAccountant = new PrivacyAccountant();
        contributionService = new AnalyticsContributionService(
                contributionRepository,
                databaseRepository,
                privacyBudgetLedgerRepository,
                privacyAccountant,
                currentUserService
        );

        owner = User.builder()
                .fullName("Hospital Custodian")
                .email("custodian@hospital.org")
                .passwordHash("hash")
                .role(User.Role.DATA_OWNER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        owner.setId(1L);

        database = FederatedDatabase.builder()
                .organizationName("City Hospital")
                .dbIdentifier("city-hosp-db")
                .owner(owner)
                .active(true)
                .registeredAt(LocalDateTime.now())
                .privacyEpsilonBudget(BigDecimal.valueOf(10.0))
                .privacyDeltaBudget(BigDecimal.valueOf(1e-5))
                .privacyEpsilonSpent(BigDecimal.ZERO)
                .build();
        database.setId(10L);
    }

    @Test
    void testSubmitContributionSuccessful() {
        when(currentUserService.requireRole(User.Role.DATA_OWNER)).thenReturn(owner);
        when(databaseRepository.findByOwnerId(1L)).thenReturn(List.of(database));

        ContributionRequest request = new ContributionRequest();
        request.setRecordCount(100);
        request.setNoisySum(BigDecimal.valueOf(4500.5));
        request.setEpsilon(BigDecimal.valueOf(1.0));
        request.setDelta(BigDecimal.valueOf(1e-5));
        request.setClippingLower(BigDecimal.valueOf(0.0));
        request.setClippingUpper(BigDecimal.valueOf(50.0));
        request.setSensitivity(BigDecimal.valueOf(50.0));
        request.setNoiseScale(BigDecimal.valueOf(2.0));
        request.setNoiseSigma(BigDecimal.valueOf(100.0)); // 50 * 2 = 100
        request.setAnomalyCount(2);
        request.setAnomalyRate(BigDecimal.valueOf(0.02));

        when(contributionRepository.save(any(AnalyticsContribution.class))).thenAnswer(inv -> {
            AnalyticsContribution c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        ContributionResponse response = contributionService.submitContribution(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(100, response.getRecordCount());
        assertEquals("GAUSSIAN", response.getMechanism());

        verify(databaseRepository).save(database);
        verify(privacyBudgetLedgerRepository).save(any(PrivacyBudgetLedger.class));
    }

    @Test
    void testSensitivityMismatchThrowsException() {
        when(currentUserService.requireRole(User.Role.DATA_OWNER)).thenReturn(owner);
        when(databaseRepository.findByOwnerId(1L)).thenReturn(List.of(database));

        ContributionRequest request = new ContributionRequest();
        request.setRecordCount(100);
        request.setNoisySum(BigDecimal.valueOf(1000.0));
        request.setEpsilon(BigDecimal.valueOf(1.0));
        request.setDelta(BigDecimal.valueOf(1e-5));
        request.setClippingLower(BigDecimal.valueOf(0.0));
        request.setClippingUpper(BigDecimal.valueOf(50.0));
        request.setSensitivity(BigDecimal.valueOf(999.0)); // Inconsistent sensitivity!
        request.setNoiseScale(BigDecimal.valueOf(1.0));
        request.setNoiseSigma(BigDecimal.valueOf(999.0));

        assertThrows(IllegalArgumentException.class, () -> contributionService.submitContribution(request));
    }

    @Test
    void testBudgetExceededThrowsException() {
        when(currentUserService.requireRole(User.Role.DATA_OWNER)).thenReturn(owner);
        // Set database budget as already exhausted
        database.setPrivacyEpsilonBudget(BigDecimal.valueOf(1.0));
        database.setPrivacyEpsilonSpent(BigDecimal.valueOf(0.99));
        // Fill RDP state with spent budget
        Map<Double, Double> heavyState = privacyAccountant.compose(privacyAccountant.emptyState(), 0.5);
        database.setRdpAccountantState(privacyAccountant.serialize(heavyState));

        when(databaseRepository.findByOwnerId(1L)).thenReturn(List.of(database));

        ContributionRequest request = new ContributionRequest();
        request.setRecordCount(50);
        request.setNoisySum(BigDecimal.valueOf(500.0));
        request.setEpsilon(BigDecimal.valueOf(2.0));
        request.setDelta(BigDecimal.valueOf(1e-5));
        request.setClippingLower(BigDecimal.valueOf(0.0));
        request.setClippingUpper(BigDecimal.valueOf(10.0));
        request.setSensitivity(BigDecimal.valueOf(10.0));
        request.setNoiseScale(BigDecimal.valueOf(0.5));
        request.setNoiseSigma(BigDecimal.valueOf(5.0));

        assertThrows(PrivacyBudgetExceededException.class, () -> contributionService.submitContribution(request));
    }
}
