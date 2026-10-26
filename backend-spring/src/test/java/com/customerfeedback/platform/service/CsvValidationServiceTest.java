package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.ValidateCsvResponse;
import com.customerfeedback.platform.exception.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CsvValidationServiceTest {

    private CsvValidationService validationService;
    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new DeduplicationService();
        validationService = new CsvValidationService(deduplicationService);
        ReflectionTestUtils.setField(validationService, "maxFileSizeBytes", 10485760L);
        ReflectionTestUtils.setField(validationService, "maxRows", 10000);
        ReflectionTestUtils.setField(validationService, "previewRowCount", 10);
        ReflectionTestUtils.setField(validationService, "minTextLength", 3);
        ReflectionTestUtils.setField(validationService, "maxTextLength", 5000);
    }

    @Test
    @DisplayName("Should reject null or empty file")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.validateFileStructure(emptyFile));
        assertEquals("EMPTY_FILE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject non-CSV file extension")
    void shouldRejectNonCsvExtension() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "abc".getBytes());
        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.validateFileStructure(txtFile));
        assertEquals("INVALID_FILE_TYPE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject file exceeding size limit")
    void shouldRejectFileExceedingSizeLimit() {
        ReflectionTestUtils.setField(validationService, "maxFileSizeBytes", 10L);
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.csv", "text/csv", "123456789012345".getBytes());
        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.validateFileStructure(largeFile));
        assertEquals("FILE_TOO_LARGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject CSV with missing required headers")
    void shouldRejectMissingRequiredHeaders() {
        String csvContent = "feedback_id,channel\nF001,web\n";
        MockMultipartFile file = new MockMultipartFile("file", "missing.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.previewAndValidate(file));
        assertEquals("MISSING_REQUIRED_HEADERS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject CSV with duplicate headers")
    void shouldRejectDuplicateHeaders() {
        String csvContent = "feedback_id,feedback_date,channel,feedback_text,channel\nF001,2026-05-21,web,Good,app\n";
        MockMultipartFile file = new MockMultipartFile("file", "duplicate_headers.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.previewAndValidate(file));
        assertEquals("DUPLICATE_HEADER", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject CSV with header only and zero data rows")
    void shouldRejectHeaderOnlyCsv() {
        String csvContent = "feedback_id,feedback_date,channel,feedback_text\n";
        MockMultipartFile file = new MockMultipartFile("file", "empty_rows.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        FileValidationException ex = assertThrows(FileValidationException.class,
                () -> validationService.previewAndValidate(file));
        assertEquals("NO_DATA_ROWS", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should preview valid CSV successfully")
    void shouldPreviewValidCsvSuccessfully() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F0001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                F0002,2026-02-14,support_ticket,Mobile Banking,5,Retail,North,Notifications are not arriving.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertTrue(response.validFile());
        assertEquals(2, response.totalRowsDetected());
        assertEquals(2, response.validRowCount());
        assertEquals(0, response.invalidRowCount());
        assertEquals(0, response.duplicateWithinFileCount());
        assertEquals(2, response.previewRows().size());
        assertTrue(response.missingHeaders().isEmpty());
    }

    @Test
    @DisplayName("Should identify row validation errors for invalid rating, date, and text")
    void shouldIdentifyRowValidationErrors() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F0001,invalid-date,web,Savings Account,2,Retail,North,Valid feedback text
                F0002,2026-02-14,support_ticket,Mobile Banking,99,Retail,North,Valid text
                F0003,2026-03-01,mobile_app,Credit Cards,4,Retail,South,no
                """;
        MockMultipartFile file = new MockMultipartFile("file", "invalid_rows.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertFalse(response.validFile());
        assertEquals(3, response.totalRowsDetected());
        assertEquals(0, response.validRowCount());
        assertEquals(3, response.invalidRowCount());
        assertEquals(3, response.validationErrors().size());

        assertTrue(response.validationErrors().stream().anyMatch(e -> "INVALID_DATE_FORMAT".equals(e.errorCode())));
        assertTrue(response.validationErrors().stream().anyMatch(e -> "INVALID_RATING_RANGE".equals(e.errorCode())));
        assertTrue(response.validationErrors().stream().anyMatch(e -> "TEXT_TOO_SHORT".equals(e.errorCode())));
    }

    @Test
    @DisplayName("Should detect duplicate rows within the same file")
    void shouldDetectDuplicateWithinFile() {
        String csvContent = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F0001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                F0001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "dup.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ValidateCsvResponse response = validationService.previewAndValidate(file);

        assertEquals(2, response.totalRowsDetected());
        assertEquals(1, response.validRowCount());
        assertEquals(1, response.duplicateWithinFileCount());
        assertFalse(response.validFile());
    }
}
