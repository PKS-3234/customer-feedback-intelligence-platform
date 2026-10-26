package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchDetailResponse;
import com.customerfeedback.platform.dto.BatchSummaryResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.dto.ProcessingErrorResponse;
import com.customerfeedback.platform.dto.ValidateCsvResponse;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.ProcessingStage;
import com.customerfeedback.platform.model.enums.SourceType;
import com.customerfeedback.platform.service.BatchIngestionService;
import com.customerfeedback.platform.service.CsvValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "Batches", description = "Endpoints for CSV batch preview, ingestion, status monitoring, and error tracking.")
public class BatchController {

    private final CsvValidationService validationService;
    private final BatchIngestionService ingestionService;

    public BatchController(CsvValidationService validationService, BatchIngestionService ingestionService) {
        this.validationService = validationService;
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate and preview CSV", description = "Parses and validates CSV structure and first rows without persisting to database.")
    @ApiResponse(responseCode = "200", description = "Validation preview results")
    @ApiResponse(responseCode = "400", description = "Malformed file or structure error")
    public ResponseEntity<ValidateCsvResponse> validateCsv(
            @Parameter(description = "CSV file to validate", required = true)
            @RequestParam("file") MultipartFile file) {
        ValidateCsvResponse response = validationService.previewAndValidate(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and ingest CSV batch", description = "Creates an ingestion batch and streams valid rows into feedback storage with deduplication.")
    @ApiResponse(responseCode = "201", description = "Batch successfully created and processed")
    @ApiResponse(responseCode = "400", description = "Invalid file or parameters")
    public ResponseEntity<BatchSummaryResponse> uploadBatch(
            @Parameter(description = "CSV file to ingest", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Source type", schema = @Schema(defaultValue = "CSV"))
            @RequestParam(value = "sourceType", required = false, defaultValue = "CSV") SourceType sourceType,
            @Parameter(description = "Uploader username or identifier")
            @RequestParam(value = "uploadedBy", required = false, defaultValue = "system") String uploadedBy) {
        BatchSummaryResponse response = ingestionService.ingestCsv(file, sourceType, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List ingestion batches", description = "Retrieves a paginated list of batches sorted by upload time.")
    public ResponseEntity<PagedResponse<BatchSummaryResponse>> listBatches(
            @RequestParam(value = "status", required = false) BatchStatus status,
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ingestionService.listBatches(status, pageable));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get batch details", description = "Retrieves details and processing metrics for a specific batch.")
    @ApiResponse(responseCode = "200", description = "Batch details found")
    @ApiResponse(responseCode = "404", description = "Batch not found")
    public ResponseEntity<BatchDetailResponse> getBatch(
            @PathVariable("batchId") UUID batchId) {
        return ResponseEntity.ok(ingestionService.getBatchDetail(batchId));
    }

    @GetMapping("/{batchId}/errors")
    @Operation(summary = "Get batch errors", description = "Retrieves paginated processing errors recorded for a specific batch.")
    public ResponseEntity<PagedResponse<ProcessingErrorResponse>> getBatchErrors(
            @PathVariable("batchId") UUID batchId,
            @RequestParam(value = "stage", required = false) ProcessingStage stage,
            @RequestParam(value = "errorCode", required = false) String errorCode,
            @PageableDefault(size = 50, sort = "csvRowNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ingestionService.listBatchErrors(batchId, stage, errorCode, pageable));
    }
}
