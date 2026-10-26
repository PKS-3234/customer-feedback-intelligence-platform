package com.customerfeedback.platform.dto;

import java.util.List;
import java.util.Map;

public record ValidateCsvResponse(
        boolean validFile,
        String fileName,
        List<String> detectedHeaders,
        List<String> requiredHeadersFound,
        List<String> missingHeaders,
        int totalRowsDetected,
        int validRowCount,
        int invalidRowCount,
        int duplicateWithinFileCount,
        List<Map<String, String>> previewRows,
        List<RowValidationErrorDto> validationErrors,
        Map<String, String> proposedMapping
) {}
