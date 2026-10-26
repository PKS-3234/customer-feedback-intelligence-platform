package com.customerfeedback.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationServiceTest {

    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new DeduplicationService();
    }

    @Test
    @DisplayName("Should produce stable SHA-256 hash for identical input")
    void shouldProduceStableHash() {
        String extRef = "F0001";
        LocalDate date = LocalDate.of(2026, 5, 21);
        String channel = "web";
        String text = "The app crashes when I open transaction history.";

        String hash1 = deduplicationService.computeFeedbackHash(extRef, date, channel, text);
        String hash2 = deduplicationService.computeFeedbackHash(extRef, date, channel, text);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should normalize whitespace and case for deduplication")
    void shouldNormalizeWhitespaceAndCase() {
        LocalDate date = LocalDate.of(2026, 5, 21);

        String hash1 = deduplicationService.computeFeedbackHash("F0001", date, "Web", "The app crashes when I open transaction history.");
        String hash2 = deduplicationService.computeFeedbackHash("  f0001  ", date, "  web  ", "The  app   crashes  when   I open transaction history.  ");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce different hashes when any key component differs")
    void shouldProduceDifferentHashesWhenComponentDiffers() {
        LocalDate date = LocalDate.of(2026, 5, 21);
        String text = "The app crashes when I open transaction history.";

        String baseHash = deduplicationService.computeFeedbackHash("F0001", date, "web", text);
        String diffExtRef = deduplicationService.computeFeedbackHash("F0002", date, "web", text);
        String diffDate = deduplicationService.computeFeedbackHash("F0001", LocalDate.of(2026, 5, 22), "web", text);
        String diffChannel = deduplicationService.computeFeedbackHash("F0001", date, "mobile_app", text);
        String diffText = deduplicationService.computeFeedbackHash("F0001", date, "web", "Different complaint");

        assertNotEquals(baseHash, diffExtRef);
        assertNotEquals(baseHash, diffDate);
        assertNotEquals(baseHash, diffChannel);
        assertNotEquals(baseHash, diffText);
    }

    @Test
    @DisplayName("Should compute file SHA-256 hash accurately")
    void shouldComputeFileHash() {
        byte[] bytes = "sample,csv,data\n1,2,3\n".getBytes(StandardCharsets.UTF_8);
        String fileHash = deduplicationService.computeFileHash(bytes);

        assertNotNull(fileHash);
        assertEquals(64, fileHash.length());
    }

    @Test
    @DisplayName("Should reject null or empty byte array for file hash")
    void shouldRejectNullOrEmptyBytesForFileHash() {
        assertThrows(IllegalArgumentException.class, () -> deduplicationService.computeFileHash(null));
        assertThrows(IllegalArgumentException.class, () -> deduplicationService.computeFileHash(new byte[0]));
    }
}
