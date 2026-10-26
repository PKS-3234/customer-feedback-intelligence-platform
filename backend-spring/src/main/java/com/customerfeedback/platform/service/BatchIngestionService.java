package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.BatchDetailResponse;
import com.customerfeedback.platform.dto.BatchSummaryResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.dto.ProcessingErrorResponse;
import com.customerfeedback.platform.dto.RowValidationErrorDto;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.entity.ProcessingError;
import com.customerfeedback.platform.exception.FileValidationException;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.ProcessingStage;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.model.enums.SourceType;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.repository.ProcessingErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class BatchIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestionService.class);

    private final IngestionBatchRepository batchRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProcessingErrorRepository errorRepository;
    private final CsvValidationService validationService;
    private final DeduplicationService deduplicationService;
    private final ObjectMapper objectMapper;

    public BatchIngestionService(IngestionBatchRepository batchRepository,
                                 FeedbackRepository feedbackRepository,
                                 ProcessingErrorRepository errorRepository,
                                 CsvValidationService validationService,
                                 DeduplicationService deduplicationService,
                                 ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.feedbackRepository = feedbackRepository;
        this.errorRepository = errorRepository;
        this.validationService = validationService;
        this.deduplicationService = deduplicationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the streaming CSV ingestion pipeline.
     */
    @Transactional
    public BatchSummaryResponse ingestCsv(MultipartFile file, SourceType sourceType, String uploadedBy) {
        validationService.validateFileStructure(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new FileValidationException("FILE_READ_ERROR", "Failed to read uploaded file: " + e.getMessage());
        }

        String fileHash = deduplicationService.computeFileHash(fileBytes);
        String fileName = file.getOriginalFilename();

        // Initialize batch
        IngestionBatch batch = new IngestionBatch(fileName, sourceType != null ? sourceType : SourceType.CSV, fileHash, uploadedBy);
        batch.setStatus(BatchStatus.IMPORTING);
        batch = batchRepository.save(batch);

        UUID batchId = batch.getBatchId();
        log.info("Started batch ingestion [ID: {}, File: {}, Hash: {}]", batchId, fileName, fileHash);

        int totalRows = 0;
        int validRows = 0;
        int successfulRows = 0;
        int failedRows = 0;
        int duplicateRows = 0;

        Set<String> seenBatchHashes = new HashSet<>();

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Reader reader = createBomAwareReader(is)) {

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);
            List<String> rawHeaders = parser.getHeaderNames();

            if (rawHeaders == null || rawHeaders.isEmpty()) {
                throw new FileValidationException("EMPTY_CSV_HEADERS", "No header row detected in CSV file.");
            }

            Map<String, String> headerMap = new LinkedHashMap<>();
            for (String h : rawHeaders) {
                String cleanHeader = stripBom(h).trim();
                headerMap.put(cleanHeader.toLowerCase(), cleanHeader);
            }

            for (CSVRecord record : parser) {
                totalRows++;

                if (isRecordCompletelyBlank(record)) {
                    continue;
                }

                int csvRowNumber = (int) record.getRecordNumber() + 1;
                Map<String, String> rawRowMap = recordToMap(record, headerMap);
                String rawRowJson = toJson(rawRowMap);

                // Row-level validation
                List<RowValidationErrorDto> validationErrors = validationService.validateRowRecord(record, headerMap, csvRowNumber);

                if (!validationErrors.isEmpty()) {
                    failedRows++;
                    for (RowValidationErrorDto err : validationErrors) {
                        ProcessingError error = new ProcessingError(
                                batchId,
                                null,
                                csvRowNumber,
                                ProcessingStage.ROW_VALIDATION,
                                err.errorCode(),
                                err.message(),
                                rawRowJson
                        );
                        errorRepository.save(error);
                    }
                    continue;
                }

                // Field extraction
                String externalRef = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_ID);
                LocalDate feedbackDate = validationService.parseDate(
                        validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_DATE));
                String channel = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_CHANNEL);
                String feedbackText = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_FEEDBACK_TEXT);
                Integer rating = validationService.parseRating(
                        validationService.getFieldValue(record, headerMap, CsvValidationService.COL_RATING));
                String product = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_PRODUCT);
                String region = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_REGION);
                String customerSegment = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_CUSTOMER_SEGMENT);
                String customerRef = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_CUSTOMER_REFERENCE);
                String language = validationService.getFieldValue(record, headerMap, CsvValidationService.COL_LANGUAGE);

                // Compute deduplication hash
                String dedupHash = deduplicationService.computeFeedbackHash(externalRef, feedbackDate, channel, feedbackText);

                // Duplicate Check 1: Duplicate within the same CSV batch
                if (seenBatchHashes.contains(dedupHash)) {
                    duplicateRows++;
                    ProcessingError error = new ProcessingError(
                            batchId,
                            null,
                            csvRowNumber,
                            ProcessingStage.DUPLICATE_CHECK,
                            "DUPLICATE_IN_FILE",
                            String.format("Duplicate feedback row in uploaded file (External Reference: '%s').", externalRef),
                            rawRowJson
                    );
                    errorRepository.save(error);
                    continue;
                }

                // Duplicate Check 2: Duplicate already stored in database from earlier batch
                if (feedbackRepository.existsByDeduplicationHash(dedupHash)) {
                    duplicateRows++;
                    ProcessingError error = new ProcessingError(
                            batchId,
                            null,
                            csvRowNumber,
                            ProcessingStage.DUPLICATE_CHECK,
                            "DUPLICATE_IN_DATABASE",
                            String.format("Feedback already stored in database with matching deduplication hash (External Reference: '%s').", externalRef),
                            rawRowJson
                    );
                    errorRepository.save(error);
                    continue;
                }

                // Row is valid and unique: persist feedback
                seenBatchHashes.add(dedupHash);
                validRows++;

                Feedback feedback = new Feedback(
                        batchId,
                        externalRef,
                        customerRef,
                        feedbackText,
                        feedbackDate,
                        channel,
                        rating,
                        product,
                        region,
                        customerSegment,
                        language,
                        dedupHash
                );
                feedback.setProcessingStatus(ProcessingStatus.AI_PENDING);
                feedbackRepository.save(feedback);
                successfulRows++;
            }

            // Determine final batch status
            BatchStatus finalStatus;
            if (successfulRows == totalRows && failedRows == 0 && duplicateRows == 0) {
                finalStatus = BatchStatus.COMPLETED;
            } else if (successfulRows > 0) {
                finalStatus = BatchStatus.COMPLETED_WITH_ERRORS;
            } else {
                finalStatus = BatchStatus.COMPLETED_WITH_ERRORS;
            }

            batch.setTotalRows(totalRows);
            batch.setValidRows(validRows);
            batch.setSuccessfulRows(successfulRows);
            batch.setFailedRows(failedRows);
            batch.setDuplicateRows(duplicateRows);
            batch.setStatus(finalStatus);
            batch = batchRepository.save(batch);

            log.info("Batch [{}] finished with status {}: total={}, success={}, failed={}, duplicate={}",
                    batchId, finalStatus, totalRows, successfulRows, failedRows, duplicateRows);

            return toSummaryResponse(batch);

        } catch (Exception e) {
            log.error("Fatal error processing batch [ID: {}]: {}", batchId, e.getMessage(), e);
            batch.setStatus(BatchStatus.FAILED);
            batch.setTotalRows(totalRows);
            batch.setValidRows(validRows);
            batch.setSuccessfulRows(successfulRows);
            batch.setFailedRows(failedRows);
            batch.setDuplicateRows(duplicateRows);
            batchRepository.save(batch);

            ProcessingError fatalError = new ProcessingError(
                    batchId,
                    null,
                    null,
                    ProcessingStage.CSV_PARSING,
                    "FATAL_PARSING_ERROR",
                    "Fatal parsing failure: " + e.getMessage(),
                    null
            );
            errorRepository.save(fatalError);

            throw new FileValidationException("CSV_INGESTION_FAILED", "Failed to ingest CSV batch: " + e.getMessage());
        }
    }

    /**
     * Lists batches with pagination and optional status filter.
     */
    @Transactional(readOnly = true)
    public PagedResponse<BatchSummaryResponse> listBatches(BatchStatus status, Pageable pageable) {
        Page<IngestionBatch> page = status != null
                ? batchRepository.findAllByStatus(status, pageable)
                : batchRepository.findAllByOrderByUploadedAtDesc(pageable);
        return PagedResponse.from(page.map(this::toSummaryResponse));
    }

    /**
     * Retrieves batch details by ID.
     */
    @Transactional(readOnly = true)
    public BatchDetailResponse getBatchDetail(UUID batchId) {
        IngestionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with ID: " + batchId));
        return toDetailResponse(batch);
    }

    /**
     * Lists errors for a batch with optional stage and error-code filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProcessingErrorResponse> listBatchErrors(UUID batchId, ProcessingStage stage, String errorCode, Pageable pageable) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Batch not found with ID: " + batchId);
        }

        Page<ProcessingError> page;
        if (stage != null && errorCode != null && !errorCode.isBlank()) {
            page = errorRepository.findAllByBatchIdAndProcessingStageAndErrorCodeOrderByCsvRowNumberAsc(batchId, stage, errorCode, pageable);
        } else if (stage != null) {
            page = errorRepository.findAllByBatchIdAndProcessingStageOrderByCsvRowNumberAsc(batchId, stage, pageable);
        } else if (errorCode != null && !errorCode.isBlank()) {
            page = errorRepository.findAllByBatchIdAndErrorCodeOrderByCsvRowNumberAsc(batchId, errorCode, pageable);
        } else {
            page = errorRepository.findAllByBatchIdOrderByCsvRowNumberAsc(batchId, pageable);
        }

        return PagedResponse.from(page.map(this::toErrorResponse));
    }

    private BatchSummaryResponse toSummaryResponse(IngestionBatch batch) {
        return new BatchSummaryResponse(
                batch.getBatchId(),
                batch.getFileName(),
                batch.getSourceType(),
                batch.getUploadedAt(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getSuccessfulRows(),
                batch.getFailedRows(),
                batch.getDuplicateRows(),
                batch.getStatus(),
                batch.getFileHash(),
                batch.getUploadedBy()
        );
    }

    private BatchDetailResponse toDetailResponse(IngestionBatch batch) {
        return new BatchDetailResponse(
                batch.getBatchId(),
                batch.getFileName(),
                batch.getSourceType(),
                batch.getUploadedAt(),
                batch.getTotalRows(),
                batch.getValidRows(),
                batch.getSuccessfulRows(),
                batch.getFailedRows(),
                batch.getDuplicateRows(),
                batch.getStatus(),
                batch.getFileHash(),
                batch.getUploadedBy(),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }

    private ProcessingErrorResponse toErrorResponse(ProcessingError error) {
        return new ProcessingErrorResponse(
                error.getErrorId(),
                error.getBatchId(),
                error.getFeedbackId(),
                error.getCsvRowNumber(),
                error.getProcessingStage(),
                error.getErrorCode(),
                error.getErrorMessage(),
                error.getOriginalRow(),
                error.getCreatedAt(),
                error.isResolved(),
                error.getResolvedAt()
        );
    }

    private Map<String, String> recordToMap(CSVRecord record, Map<String, String> headerMap) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            String colName = entry.getValue();
            if (record.isMapped(colName)) {
                map.put(colName, record.get(colName));
            }
        }
        return map;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isRecordCompletelyBlank(CSVRecord record) {
        for (String val : record) {
            if (val != null && !val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Reader createBomAwareReader(InputStream is) throws IOException {
        PushbackInputStream pbis = new PushbackInputStream(new BufferedInputStream(is), 4);
        byte[] bom = new byte[3];
        int n = pbis.read(bom, 0, bom.length);
        if (n == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            // Bom consumed
        } else if (n > 0) {
            pbis.unread(bom, 0, n);
        }
        return new InputStreamReader(pbis, StandardCharsets.UTF_8);
    }

    private String stripBom(String s) {
        if (s == null) return "";
        if (s.startsWith("\uFEFF")) {
            return s.substring(1);
        }
        return s;
    }
}
