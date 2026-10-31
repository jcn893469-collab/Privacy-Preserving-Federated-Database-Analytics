package com.fpa.api.controller;

import com.fpa.domain.entity.DatasetColumnSchema;
import com.fpa.domain.entity.DatasetMetadata;
import com.fpa.domain.entity.User;
import com.fpa.api.dto.DatasetColumnRequest;
import com.fpa.api.dto.DatasetColumnResponse;
import com.fpa.api.dto.DatasetCreateRequest;
import com.fpa.api.dto.DatasetResponse;
import com.fpa.api.dto.DatasetUpdateRequest;
import com.fpa.security.CurrentUserService;
import com.fpa.service.FederatedDatabaseService;
import com.fpa.service.DatasetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;
    private final FederatedDatabaseService federatedDatabaseService;
    private final CurrentUserService currentUserService;

    public DatasetController(DatasetService datasetService,
                             FederatedDatabaseService federatedDatabaseService,
                             CurrentUserService currentUserService) {
        this.datasetService = datasetService;
        this.federatedDatabaseService = federatedDatabaseService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<DatasetResponse> registerDataset(@Valid @RequestBody DatasetCreateRequest request) {
        DatasetMetadata dataset = datasetService.registerDataset(
            request.getDatabaseId(), request.getDatasetName(), request.getDescription(), request.getTotalRecords()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasetResponse.from(dataset));
    }

    @GetMapping("/database/{databaseId}")
    public ResponseEntity<?> getDatasetsForDatabase(
            @PathVariable Long databaseId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            federatedDatabaseService.requireOwned(databaseId, caller);
        }
        if (hasText(query) || page != null || size != null) {
            Pageable pageable = datasetPageRequest(page, size);
            return ResponseEntity.ok(datasetService.searchDatasets(databaseId, query, pageable)
                .map(DatasetResponse::from));
        }
        return ResponseEntity.ok(datasetService.getDatasetsForDatabase(databaseId).stream()
            .map(DatasetResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{datasetId}")
    public ResponseEntity<DatasetResponse> getDataset(@PathVariable Long datasetId) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        datasetService.verifyDatasetAccess(datasetId, caller);
        return ResponseEntity.ok(DatasetResponse.from(datasetService.getDataset(datasetId)));
    }

    @PutMapping("/{datasetId}")
    public ResponseEntity<DatasetResponse> updateDataset(
            @PathVariable Long datasetId,
            @Valid @RequestBody DatasetUpdateRequest request) {
        return ResponseEntity.ok(DatasetResponse.from(datasetService.updateDataset(datasetId, request)));
    }

    @PostMapping("/{datasetId}/columns")
    public ResponseEntity<DatasetColumnResponse> addColumn(
            @PathVariable Long datasetId,
            @Valid @RequestBody DatasetColumnRequest request) {
        DatasetColumnSchema col = datasetService.registerColumn(datasetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasetColumnResponse.from(col));
    }

    @PutMapping("/{datasetId}/columns/{columnId}")
    public ResponseEntity<DatasetColumnResponse> updateColumn(
            @PathVariable Long datasetId,
            @PathVariable Long columnId,
            @Valid @RequestBody DatasetColumnRequest request) {
        return ResponseEntity.ok(DatasetColumnResponse.from(datasetService.updateColumn(datasetId, columnId, request)));
    }

    @GetMapping("/{datasetId}/columns")
    public ResponseEntity<List<DatasetColumnResponse>> getColumns(@PathVariable Long datasetId) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        datasetService.verifyDatasetAccess(datasetId, caller);
        return ResponseEntity.ok(datasetService.getColumnsForDataset(datasetId).stream()
            .map(DatasetColumnResponse::from).collect(Collectors.toList()));
    }

    @DeleteMapping("/{datasetId}/columns/{columnId}")
    public ResponseEntity<?> deleteColumn(@PathVariable Long datasetId, @PathVariable Long columnId) {
        datasetService.deleteColumn(datasetId, columnId);
        return ResponseEntity.ok(Map.of("message", "Dataset column deleted successfully", "id", columnId));
    }

    @DeleteMapping("/{datasetId}")
    public ResponseEntity<?> deleteDataset(@PathVariable Long datasetId) {
        datasetService.deleteDataset(datasetId);
        return ResponseEntity.ok(Map.of("message", "Dataset deleted successfully", "id", datasetId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Pageable datasetPageRequest(Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
    }
}
