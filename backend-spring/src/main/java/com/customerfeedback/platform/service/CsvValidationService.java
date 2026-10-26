package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.RowValidationErrorDto;
import com.customerfeedback.platform.dto.ValidateCsvResponse;
import com.customerfeedback.platform.exception.FileValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvValidationService {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationService.class);

    public static final String COL_FEEDBACK_ID = "feedback_id";
    public static final String COL_FEEDBACK_DATE = "feedback_date";
    public static final String COL_CHANNEL = "channel";
    public static final String COL_FEEDBACK_TEXT = "feedback_text";
    public static final String COL_RATING = "rating";
    public static final String COL_PRODUCT = "product";
    public static final String COL_REGION = "region";
    public static final String COL_CUSTOMER_SEGMENT = "customer_segment";
    public static final String COL_CUSTOMER_REFERENCE = "customer_reference";
    public static final String COL_LANGUAGE = "language";

    public static final Set<String> REQUIRED_COLUMNS = Set.of(
            COL_FEEDBACK_ID,
            COL_FEEDBACK_DATE,
            COL_CHANNEL,
            COL_FEEDBACK_TEXT
    );

    private final DeduplicationService deduplicationService;

    @Value("${app.ingestion.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Value("${app.ingestion.max-rows:10000}")
    private int maxRows;

    @Value("${app.ingestion.preview-rows:10}")
    private int previewRowCount;

    @Value("${app.ingestion.min-text-length:3}")
    private int minTextLength;

    @Value("${app.ingestion.max-text-length:5000}")
    private int maxTextLength;

    public CsvValidationService(DeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    /**
     * Performs file-level validation on the uploaded multipart file.
     */
    public void validateFileStructure(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("EMPTY_FILE", "The uploaded file is empty or missing.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new FileValidationException("INVALID_FILE_TYPE", "File must have a .csv extension.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new FileValidationException("FILE_TOO_LARGE",
                    String.format("File size (%d bytes) exceeds the allowed limit of %d bytes.", file.getSize(), maxFileSizeBytes));
        }
    }

    /**
     * Previews and validates CSV contents without persisting records.
     */
    public ValidateCsvResponse previewAndValidate(MultipartFile file) {
        validateFileStructure(file);

        try (InputStream is = file.getInputStream();
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
                throw new FileValidationException("EMPTY_CSV_HEADERS", "The CSV file does not contain a valid header row.");
            }

            // Normalize header names for comparison
            Map<String, String> normalizedHeaderMap = new LinkedHashMap<>();
            List<String> detectedHeaders = new ArrayList<>();
            Set<String> seenHeaders = new HashSet<>();

            for (String header : rawHeaders) {
                String cleanHeader = stripBom(header).trim();
                String lowerHeader = cleanHeader.toLowerCase();

                if (seenHeaders.contains(lowerHeader)) {
                    throw new FileValidationException("DUPLICATE_HEADER",
                            String.format("Duplicate header column detected: '%s'", cleanHeader));
                }
                seenHeaders.add(lowerHeader);
                detectedHeaders.add(cleanHeader);
                normalizedHeaderMap.put(lowerHeader, cleanHeader);
            }

            // Check required headers
            List<String> requiredFound = new ArrayList<>();
            List<String> missingHeaders = new ArrayList<>();

            for (String req : REQUIRED_COLUMNS) {
                if (normalizedHeaderMap.containsKey(req)) {
                    requiredFound.add(normalizedHeaderMap.get(req));
                } else {
                    missingHeaders.add(req);
                }
            }

            if (!missingHeaders.isEmpty()) {
                throw new FileValidationException("MISSING_REQUIRED_HEADERS",
                        String.format("Missing required CSV header(s): %s", String.join(", ", missingHeaders)));
            }

            // Proposed mapping for canonical data
            Map<String, String> proposedMapping = createProposedMapping(normalizedHeaderMap);

            List<Map<String, String>> previewRows = new ArrayList<>();
            List<RowValidationErrorDto> validationErrors = new ArrayList<>();
            Set<String> seenFileHashes = new HashSet<>();
            int totalRows = 0;
            int validRows = 0;
            int duplicateCount = 0;

            for (CSVRecord record : parser) {
                totalRows++;

                if (totalRows > maxRows) {
                    throw new FileValidationException("ROW_LIMIT_EXCEEDED",
                            String.format("CSV row count exceeds the configured maximum limit of %d rows.", maxRows));
                }

                // Check for completely blank line
                if (isRecordCompletelyBlank(record)) {
                    continue;
                }

                int csvRowNumber = (int) record.getRecordNumber() + 1; // 1-based, +1 accounting for header

                // Validate row fields
                List<RowValidationErrorDto> rowErrors = validateRowRecord(record, normalizedHeaderMap, csvRowNumber);
                boolean isRowValid = rowErrors.isEmpty();

                if (isRowValid) {
                    // Check duplicate within file
                    String extRef = getFieldValue(record, normalizedHeaderMap, COL_FEEDBACK_ID);
                    LocalDate date = parseDate(getFieldValue(record, normalizedHeaderMap, COL_FEEDBACK_DATE));
                    String channel = getFieldValue(record, normalizedHeaderMap, COL_CHANNEL);
                    String text = getFieldValue(record, normalizedHeaderMap, COL_FEEDBACK_TEXT);

                    String dedupHash = deduplicationService.computeFeedbackHash(extRef, date, channel, text);
                    if (seenFileHashes.contains(dedupHash)) {
                        duplicateCount++;
                        rowErrors.add(new RowValidationErrorDto(
                                csvRowNumber,
                                "composite_key",
                                "DUPLICATE_IN_FILE",
                                "Duplicate record detected within the same CSV file."
                        ));
                        isRowValid = false;
                    } else {
                        seenFileHashes.add(dedupHash);
                        validRows++;
                    }
                }

                if (!rowErrors.isEmpty()) {
                    validationErrors.addAll(rowErrors);
                }

                // Add to preview rows if under preview limit
                if (previewRows.size() < previewRowCount) {
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (Map.Entry<String, String> entry : normalizedHeaderMap.entrySet()) {
                        String rawVal = record.isMapped(entry.getValue()) ? record.get(entry.getValue()) : "";
                        rowMap.put(entry.getValue(), rawVal);
                    }
                    previewRows.add(rowMap);
                }
            }

            if (totalRows == 0) {
                throw new FileValidationException("NO_DATA_ROWS", "The CSV file has headers but contains zero data rows.");
            }

            int invalidRows = totalRows - validRows;
            boolean isValidFile = validationErrors.isEmpty();

            return new ValidateCsvResponse(
                    isValidFile,
                    file.getOriginalFilename(),
                    detectedHeaders,
                    requiredFound,
                    missingHeaders,
                    totalRows,
                    validRows,
                    invalidRows,
                    duplicateCount,
                    previewRows,
                    validationErrors,
                    proposedMapping
            );

        } catch (FileValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", e.getMessage(), e);
            throw new FileValidationException("MALFORMED_CSV", "Malformed CSV format: " + e.getMessage());
        }
    }

    /**
     * Validates a single CSV record against business rules.
     */
    public List<RowValidationErrorDto> validateRowRecord(CSVRecord record, Map<String, String> headerMap, int rowNumber) {
        List<RowValidationErrorDto> errors = new ArrayList<>();

        // 1. feedback_id
        String feedbackId = getFieldValue(record, headerMap, COL_FEEDBACK_ID);
        if (feedbackId == null || feedbackId.trim().isEmpty()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_ID, "MISSING_FIELD", "Required field 'feedback_id' is missing."));
        }

        // 2. feedback_text
        String feedbackText = getFieldValue(record, headerMap, COL_FEEDBACK_TEXT);
        if (feedbackText == null || feedbackText.trim().isEmpty()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT, "MISSING_FIELD", "Required field 'feedback_text' is missing."));
        } else {
            String trimmedText = feedbackText.trim();
            if (trimmedText.length() < minTextLength) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT, "TEXT_TOO_SHORT",
                        String.format("Feedback text length (%d) is shorter than minimum (%d).", trimmedText.length(), minTextLength)));
            } else if (trimmedText.length() > maxTextLength) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_TEXT, "TEXT_TOO_LONG",
                        String.format("Feedback text length (%d) exceeds maximum (%d).", trimmedText.length(), maxTextLength)));
            }
        }

        // 3. feedback_date
        String feedbackDateStr = getFieldValue(record, headerMap, COL_FEEDBACK_DATE);
        if (feedbackDateStr == null || feedbackDateStr.trim().isEmpty()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_DATE, "MISSING_FIELD", "Required field 'feedback_date' is missing."));
        } else {
            try {
                LocalDate.parse(feedbackDateStr.trim());
            } catch (DateTimeParseException e) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_FEEDBACK_DATE, "INVALID_DATE_FORMAT",
                        String.format("Invalid date format '%s'. Expected ISO-8601 (YYYY-MM-DD).", feedbackDateStr)));
            }
        }

        // 4. channel
        String channel = getFieldValue(record, headerMap, COL_CHANNEL);
        if (channel == null || channel.trim().isEmpty()) {
            errors.add(new RowValidationErrorDto(rowNumber, COL_CHANNEL, "MISSING_FIELD", "Required field 'channel' is missing."));
        }

        // 5. rating (optional)
        String ratingStr = getFieldValue(record, headerMap, COL_RATING);
        if (ratingStr != null && !ratingStr.trim().isEmpty()) {
            try {
                int rating = Integer.parseInt(ratingStr.trim());
                if (rating < 1 || rating > 5) {
                    errors.add(new RowValidationErrorDto(rowNumber, COL_RATING, "INVALID_RATING_RANGE",
                            String.format("Rating value %d is out of range. Must be between 1 and 5.", rating)));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowValidationErrorDto(rowNumber, COL_RATING, "INVALID_RATING_FORMAT",
                        String.format("Rating value '%s' must be an integer between 1 and 5.", ratingStr)));
            }
        }

        return errors;
    }

    public String getFieldValue(CSVRecord record, Map<String, String> headerMap, String canonicalName) {
        String actualHeader = headerMap.get(canonicalName.toLowerCase());
        if (actualHeader != null && record.isMapped(actualHeader)) {
            String val = record.get(actualHeader);
            return val != null ? val.trim() : null;
        }
        return null;
    }

    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public Integer parseRating(String ratingStr) {
        if (ratingStr == null || ratingStr.trim().isEmpty()) {
            return null;
        }
        try {
            int val = Integer.parseInt(ratingStr.trim());
            return (val >= 1 && val <= 5) ? val : null;
        } catch (Exception e) {
            return null;
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
            // Found UTF-8 BOM, consume it
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

    private Map<String, String> createProposedMapping(Map<String, String> headerMap) {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put(COL_FEEDBACK_ID, "external_reference");
        mapping.put(COL_FEEDBACK_DATE, "feedback_date");
        mapping.put(COL_CHANNEL, "channel");
        mapping.put(COL_FEEDBACK_TEXT, "feedback_text");
        mapping.put(COL_RATING, "rating");
        mapping.put(COL_PRODUCT, "product");
        mapping.put(COL_REGION, "region");
        mapping.put(COL_CUSTOMER_SEGMENT, "customer_segment");
        mapping.put(COL_CUSTOMER_REFERENCE, "customer_reference");
        mapping.put(COL_LANGUAGE, "language");
        return mapping;
    }
}
