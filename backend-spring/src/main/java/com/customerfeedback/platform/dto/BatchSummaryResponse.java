package com.customerfeedback.platform.dto;

import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.model.enums.SourceType;

import java.time.Instant;
import java.util.UUID;

public record BatchSummaryResponse(
        UUID batchId,
        String fileName,
        SourceType sourceType,
        Instant uploadedAt,
        int totalRows,
        int validRows,
        int successfulRows,
        int failedRows,
        int duplicateRows,
        BatchStatus status,
        String fileHash,
        String uploadedBy
) {}
