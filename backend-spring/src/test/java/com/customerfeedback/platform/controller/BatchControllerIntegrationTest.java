package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.model.enums.BatchStatus;
import com.customerfeedback.platform.repository.FeedbackRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.repository.ProcessingErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionBatchRepository batchRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ProcessingErrorRepository errorRepository;

    @BeforeEach
    void cleanUp() {
        errorRepository.deleteAll();
        feedbackRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("Should validate CSV without persisting records")
    void shouldValidateCsvWithoutPersisting() throws Exception {
        String csv = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F1001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                F1002,2026-02-14,support_ticket,Mobile Banking,4,Retail,North,Notifications are not arriving.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "preview.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/batches/validate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validFile", is(true)))
                .andExpect(jsonPath("$.totalRowsDetected", is(2)))
                .andExpect(jsonPath("$.validRowCount", is(2)))
                .andExpect(jsonPath("$.invalidRowCount", is(0)))
                .andExpect(jsonPath("$.previewRows", hasSize(2)));

        assertEquals(0, batchRepository.count());
        assertEquals(0, feedbackRepository.count());
    }

    @Test
    @DisplayName("Should ingest valid CSV, persist feedback rows, and return batch summary")
    void shouldIngestValidCsv() throws Exception {
        String csv = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F2001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                F2002,2026-02-14,support_ticket,Mobile Banking,4,Retail,North,Notifications are not arriving.
                """;
        MockMultipartFile file = new MockMultipartFile("file", "test_batch.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/batches").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId", notNullValue()))
                .andExpect(jsonPath("$.totalRows", is(2)))
                .andExpect(jsonPath("$.validRows", is(2)))
                .andExpect(jsonPath("$.successfulRows", is(2)))
                .andExpect(jsonPath("$.failedRows", is(0)))
                .andExpect(jsonPath("$.duplicateRows", is(0)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        assertEquals(1, batchRepository.count());
        assertEquals(2, feedbackRepository.count());
    }

    @Test
    @DisplayName("Should detect duplicate rows across two uploads idempotently")
    void shouldDetectDuplicateRowsAcrossTwoUploads() throws Exception {
        String csv = """
                feedback_id,feedback_date,channel,product,rating,customer_segment,region,feedback_text
                F3001,2026-05-21,web,Savings Account,2,Retail,North,The app crashes when I open transaction history.
                F3002,2026-02-14,support_ticket,Mobile Banking,4,Retail,North,Notifications are not arriving.
                """;
        MockMultipartFile file1 = new MockMultipartFile("file", "batch1.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file2 = new MockMultipartFile("file", "batch2.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        // First upload
        mockMvc.perform(multipart("/api/v1/batches").file(file1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.successfulRows", is(2)));

        assertEquals(2, feedbackRepository.count());

        // Second upload: duplicate detection across batches
        mockMvc.perform(multipart("/api/v1/batches").file(file2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED_WITH_ERRORS")))
                .andExpect(jsonPath("$.totalRows", is(2)))
                .andExpect(jsonPath("$.successfulRows", is(0)))
                .andExpect(jsonPath("$.duplicateRows", is(2)));

        // Still exactly 2 feedback records in the database - no duplicate records inserted!
        assertEquals(2, feedbackRepository.count());
        assertEquals(2, batchRepository.count());
    }

    @Test
    @DisplayName("Should retrieve paginated batch list and batch detail")
    void shouldRetrieveBatchesAndDetail() throws Exception {
        IngestionBatch batch = new IngestionBatch("file1.csv", null, "hash1", "admin");
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setTotalRows(10);
        batch.setSuccessfulRows(10);
        batch = batchRepository.save(batch);

        mockMvc.perform(get("/api/v1/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fileName", is("file1.csv")));

        mockMvc.perform(get("/api/v1/batches/" + batch.getBatchId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId", is(batch.getBatchId().toString())))
                .andExpect(jsonPath("$.fileName", is("file1.csv")))
                .andExpect(jsonPath("$.totalRows", is(10)));
    }

    @Test
    @DisplayName("Should return 404 for non-existent batch ID")
    void shouldReturn404ForUnknownBatch() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/batches/" + randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @Test
    @DisplayName("Should query feedback records with filtering")
    void shouldQueryFeedbackWithFilters() throws Exception {
        IngestionBatch batch = new IngestionBatch("file.csv", null, "hash", "tester");
        batch = batchRepository.save(batch);

        Feedback fb = new Feedback(
                batch.getBatchId(), "F9999", "C123",
                "Great experience with savings account",
                java.time.LocalDate.of(2026, 6, 15),
                "web", 5, "Savings Account", "North", "Retail", "en", "hash999"
        );
        feedbackRepository.save(fb);

        mockMvc.perform(get("/api/v1/feedback")
                        .param("channel", "web")
                        .param("keyword", "savings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalReference", is("F9999")));
    }
}
