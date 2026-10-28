package com.fpa.api.controller;

import com.fpa.domain.entity.DatasetColumnSchema;
import com.fpa.domain.entity.DatasetMetadata;
import com.fpa.domain.entity.User;
import com.fpa.security.CurrentUserService;
import com.fpa.service.FederatedDatabaseService;
import com.fpa.service.DatasetService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> registerDataset(@RequestBody Map<String, Object> body) {
        Long databaseId = Long.valueOf(body.get("databaseId").toString());
        String datasetName = (String) body.get("datasetName");
        String description = (String) body.get("description");
        Integer totalRecords = body.get("totalRecords") != null ? Integer.valueOf(body.get("totalRecords").toString()) : 0;

        DatasetMetadata dataset = datasetService.registerDataset(databaseId, datasetName, description, totalRecords);
        return ResponseEntity.status(HttpStatus.CREATED).body(dataset);
    }

    @GetMapping("/database/{databaseId}")
    public ResponseEntity<?> getDatasetsForDatabase(
            @PathVariable Long databaseId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        if (caller.getRole() == User.Role.DATA_OWNER) {
            federatedDatabaseService.requireOwned(databaseId, caller);
        }
        if (query != null && !query.isBlank()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return ResponseEntity.ok(datasetService.searchDatasets(databaseId, query, pageable));
        }
        return ResponseEntity.ok(datasetService.getDatasetsForDatabase(databaseId));
    }

    @PostMapping("/{datasetId}/columns")
    public ResponseEntity<?> addColumn(
            @PathVariable Long datasetId,
            @RequestBody Map<String, Object> body) {
        String columnName = (String) body.get("columnName");
        String dataType = (String) body.getOrDefault("dataType", "NUMERIC");
        BigDecimal lower = new BigDecimal(body.get("clippingLower").toString());
        BigDecimal upper = new BigDecimal(body.get("clippingUpper").toString());

        DatasetColumnSchema col = datasetService.registerColumn(datasetId, columnName, dataType, lower, upper);
        return ResponseEntity.status(HttpStatus.CREATED).body(col);
    }

    @GetMapping("/{datasetId}/columns")
    public ResponseEntity<?> getColumns(@PathVariable Long datasetId) {
        User caller = currentUserService.requireAnyRole(User.Role.ADMIN, User.Role.DATA_OWNER);
        datasetService.verifyDatasetAccess(datasetId, caller);
        return ResponseEntity.ok(datasetService.getColumnsForDataset(datasetId));
    }

    @DeleteMapping("/{datasetId}")
    public ResponseEntity<?> deleteDataset(@PathVariable Long datasetId) {
        datasetService.deleteDataset(datasetId);
        return ResponseEntity.ok(Map.of("message", "Dataset deleted successfully", "id", datasetId));
    }
}
