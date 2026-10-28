package com.fpa.service;

import com.fpa.api.dto.ContributionRequest;
import com.fpa.api.dto.ContributionResponse;
import com.fpa.api.dto.FederatedResultResponse;
import com.fpa.api.dto.PrivacyStatusResponse;
import com.fpa.domain.entity.AnalyticsContribution;
import com.fpa.domain.entity.FederatedDatabase;
import com.fpa.domain.entity.PrivacyBudgetLedger;
import com.fpa.domain.entity.User;
import com.fpa.domain.repository.AnalyticsContributionRepository;
import com.fpa.domain.repository.FederatedDatabaseRepository;
import com.fpa.domain.repository.PrivacyBudgetLedgerRepository;
import com.fpa.privacy.ClippingUtil;
import com.fpa.privacy.PrivacyAccountant;
import com.fpa.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnalyticsContributionService {

    private final AnalyticsContributionRepository analyticsContributionRepository;
    private final FederatedDatabaseRepository federatedDatabaseRepository;
    private final PrivacyBudgetLedgerRepository privacyBudgetLedgerRepository;
    private final PrivacyAccountant privacyAccountant;
    private final CurrentUserService currentUserService;

    public AnalyticsContributionService(AnalyticsContributionRepository analyticsContributionRepository,
                                         FederatedDatabaseRepository federatedDatabaseRepository,
                                         PrivacyBudgetLedgerRepository privacyBudgetLedgerRepository,
                                         PrivacyAccountant privacyAccountant,
                                         CurrentUserService currentUserService) {
        this.analyticsContributionRepository = analyticsContributionRepository;
        this.federatedDatabaseRepository = federatedDatabaseRepository;
        this.privacyBudgetLedgerRepository = privacyBudgetLedgerRepository;
        this.privacyAccountant = privacyAccountant;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ContributionResponse submitContribution(ContributionRequest request) {
        User owner = currentUserService.requireRole(User.Role.DATA_OWNER);
        return submitOwnedContribution(request, owner);
    }

    @Transactional
    public ContributionResponse submitOwnedContribution(ContributionRequest request, User owner) {
        List<FederatedDatabase> owned = federatedDatabaseRepository.findByOwnerId(owner.getId());
        if (owned.isEmpty()) {
            throw new IllegalArgumentException("No organization database is registered for this account");
        }
        FederatedDatabase database = owned.get(0);
        return persistContribution(request, database);
    }

    @Transactional
    public ContributionResponse submitContributionForDatabase(ContributionRequest request, FederatedDatabase database) {
        return persistContribution(request, database);
    }

    private ContributionResponse persistContribution(ContributionRequest request, FederatedDatabase database) {
        if (!database.isActive()) {
            throw new IllegalArgumentException("Organization is not active and cannot submit contributions");
        }

        double clipLower = request.getClippingLower().doubleValue();
        double clipUpper = request.getClippingUpper().doubleValue();
        double expectedSensitivity = ClippingUtil.sumSensitivity(clipLower, clipUpper);
        if (Math.abs(expectedSensitivity - request.getSensitivity().doubleValue()) > 1e-4) {
            throw new IllegalArgumentException("sensitivity must equal clippingUpper - clippingLower");
        }

        double expectedSigma = request.getSensitivity().doubleValue() * request.getNoiseScale().doubleValue();
        if (Math.abs(expectedSigma - request.getNoiseSigma().doubleValue()) > 1e-2) {
            throw new IllegalArgumentException("noiseSigma must equal sensitivity * noiseScale");
        }

        double delta = request.getDelta().doubleValue();
        double orgDelta = database.getPrivacyDeltaBudget().doubleValue();
        if (Math.abs(delta - orgDelta) > 1e-12) {
            delta = orgDelta;
        }

        BigDecimal preSpent = database.getPrivacyEpsilonSpent();

        Map<Double, Double> current = privacyAccountant.deserialize(database.getRdpAccountantState());
        Map<Double, Double> updated = privacyAccountant.checkAndCompose(
            current,
            request.getNoiseScale().doubleValue(),
            database.getPrivacyEpsilonBudget().doubleValue(),
            delta
        );
        double spent = privacyAccountant.rdpToEpsilon(updated, delta);

        database.setRdpAccountantState(privacyAccountant.serialize(updated));
        BigDecimal postSpent = BigDecimal.valueOf(spent).setScale(4, RoundingMode.HALF_UP);
        database.setPrivacyEpsilonSpent(postSpent);
        federatedDatabaseRepository.save(database);

        double roundCost = privacyAccountant.rdpToEpsilon(
            privacyAccountant.compose(privacyAccountant.emptyState(), request.getNoiseScale().doubleValue()),
            delta
        );
        BigDecimal roundEpsilonCost = BigDecimal.valueOf(roundCost).setScale(4, RoundingMode.HALF_UP);

        AnalyticsContribution contribution = AnalyticsContribution.builder()
            .database(database)
            .recordCount(request.getRecordCount())
            .noisySum(request.getNoisySum())
            .epsilon(request.getEpsilon())
            .deltaValue(BigDecimal.valueOf(delta))
            .mechanism("GAUSSIAN")
            .clippingLower(request.getClippingLower())
            .clippingUpper(request.getClippingUpper())
            .sensitivity(request.getSensitivity())
            .noiseSigma(request.getNoiseSigma())
            .noiseScale(request.getNoiseScale())
            .rdpEpsilonCost(roundEpsilonCost)
            .anomalyCount(request.getAnomalyCount())
            .anomalyRate(request.getAnomalyRate())
            .submittedAt(LocalDateTime.now())
            .build();

        AnalyticsContribution savedContribution = analyticsContributionRepository.save(contribution);

        // Record entry in Privacy Budget Ledger (Audit Trail)
        PrivacyBudgetLedger ledgerEntry = PrivacyBudgetLedger.builder()
            .database(database)
            .contribution(savedContribution)
            .preEpsilonSpent(preSpent)
            .roundEpsilonCost(roundEpsilonCost)
            .postEpsilonSpent(postSpent)
            .noiseScale(request.getNoiseScale())
            .noiseSigma(request.getNoiseSigma())
            .evaluatedAt(LocalDateTime.now())
            .build();
        privacyBudgetLedgerRepository.save(ledgerEntry);

        return toResponse(savedContribution);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> getAllContributions() {
        return analyticsContributionRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> getPaginatedContributions(Pageable pageable) {
        return analyticsContributionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> searchAndFilterContributions(
            Long databaseId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return analyticsContributionRepository.searchAndFilter(
            databaseId != null, databaseId, startDate != null, startDate, endDate != null, endDate, pageable
        )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> searchGlobalContributions(String query, Pageable pageable) {
        return analyticsContributionRepository.searchGlobalContributions(query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> getContributionsOwnedBy(User owner) {
        return federatedDatabaseRepository.findByOwnerId(owner.getId()).stream()
            .flatMap(db -> analyticsContributionRepository.findByDatabaseId(db.getId()).stream())
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> getPaginatedContributionsOwnedBy(User owner, Pageable pageable) {
        List<Long> databaseIds = federatedDatabaseRepository.findByOwnerId(owner.getId()).stream()
            .map(FederatedDatabase::getId)
            .toList();
        if (databaseIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return analyticsContributionRepository.findByDatabaseIdIn(databaseIds, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ContributionResponse> searchOwnedContributions(
            User owner, Long databaseId, LocalDateTime startDate, LocalDateTime endDate,
            String query, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
        return analyticsContributionRepository.searchOwned(
            owner.getId(), databaseId != null, databaseId,
            startDate != null, startDate, endDate != null, endDate, normalizedQuery, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContributionResponse getContributionById(Long id) {
        return toResponse(analyticsContributionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Contribution not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public ContributionResponse getContributionById(Long id, User caller) {
        AnalyticsContribution contribution = analyticsContributionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Contribution not found with id: " + id));
        if (caller.getRole() != User.Role.ADMIN
                && !contribution.getDatabase().getOwner().getId().equals(caller.getId())) {
            throw new SecurityException("You do not have permission to view this contribution");
        }
        return toResponse(contribution);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> getContributionsByDatabaseId(Long databaseId) {
        return analyticsContributionRepository.findByDatabaseId(databaseId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteContribution(Long id) {
        User user = currentUserService.requireUser();
        AnalyticsContribution contribution = analyticsContributionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Contribution not found with id: " + id));

        if (user.getRole() != User.Role.ADMIN && !contribution.getDatabase().getOwner().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to delete this contribution");
        }
        analyticsContributionRepository.delete(contribution);
    }

    @Transactional(readOnly = true)
    public FederatedResultResponse computeGlobalFederatedResult() {
        return aggregate(analyticsContributionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public FederatedResultResponse computeFederatedResult(Long databaseId) {
        if (!federatedDatabaseRepository.existsById(databaseId)) {
            throw new IllegalArgumentException("Database not found with id: " + databaseId);
        }
        return aggregate(analyticsContributionRepository.findByDatabaseId(databaseId));
    }

    @Transactional(readOnly = true)
    public PrivacyStatusResponse privacyStatusFor(FederatedDatabase database) {
        int count = analyticsContributionRepository.findByDatabaseId(database.getId()).size();
        return new PrivacyStatusResponse(
            database.getId(),
            database.getOrganizationName(),
            database.getPrivacyEpsilonBudget(),
            database.getPrivacyEpsilonSpent(),
            database.getPrivacyDeltaBudget(),
            count
        );
    }

    private FederatedResultResponse aggregate(List<AnalyticsContribution> contributions) {
        FederatedResultResponse response = new FederatedResultResponse();
        response.setMechanism("GAUSSIAN + RDP");
        if (contributions.isEmpty()) {
            response.setPrivacySummary("No privacy-protected contributions have been submitted yet.");
            return response;
        }

        int totalCount = 0;
        BigDecimal totalSum = BigDecimal.ZERO;
        for (AnalyticsContribution contribution : contributions) {
            totalCount += contribution.getRecordCount();
            totalSum = totalSum.add(contribution.getNoisySum());
        }

        List<String> names = contributions.stream()
            .map(c -> c.getDatabase().getOrganizationName())
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        double mean = totalCount > 0
            ? totalSum.divide(BigDecimal.valueOf(totalCount), 6, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        response.setParticipatingOrganizations(names.size());
        response.setParticipatingOrganizationNames(names);
        response.setContributionCount(contributions.size());
        response.setProtectedTotalCount(totalCount);
        response.setProtectedAggregateSum(totalSum.setScale(4, RoundingMode.HALF_UP).doubleValue());
        response.setGlobalProtectedMean(mean);
        response.setPrivacySummary(
            "Central aggregation uses only Gaussian-noised sums. Raw records are not stored. "
                + "Mean is protectedSum / protectedCount, so noise from each contribution remains in the result."
        );
        return response;
    }

    public ContributionResponse toResponse(AnalyticsContribution contribution) {
        return new ContributionResponse(
            contribution.getId(),
            contribution.getDatabase().getId(),
            contribution.getDatabase().getDbIdentifier(),
            contribution.getDatabase().getOrganizationName(),
            contribution.getRecordCount(),
            contribution.getNoisySum(),
            contribution.getEpsilon(),
            contribution.getDeltaValue(),
            contribution.getMechanism(),
            contribution.getSensitivity(),
            contribution.getNoiseSigma(),
            contribution.getAnomalyCount(),
            contribution.getAnomalyRate(),
            contribution.getSubmittedAt()
        );
    }
}
