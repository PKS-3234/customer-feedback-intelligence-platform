package com.customerfeedback.platform.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DeduplicationService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Computes a SHA-256 hexadecimal hash over raw file bytes.
     */
    public String computeFileHash(byte[] content) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be null or empty when computing hash");
        }
        return sha256(content);
    }

    /**
     * Computes the canonical deduplication hash for a feedback record.
     * Composite input: normalized externalReference | normalized feedbackDate | normalized channel | normalized feedbackText
     */
    public String computeFeedbackHash(String externalReference, LocalDate feedbackDate, String channel, String feedbackText) {
        String normExtRef = normalizeText(externalReference);
        String normDate = feedbackDate != null ? feedbackDate.format(ISO_FORMATTER) : "";
        String normChannel = normalizeText(channel);
        String normText = normalizeFeedbackText(feedbackText);

        String composite = normExtRef + "|" + normDate + "|" + normChannel + "|" + normText;
        return sha256(composite.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Normalizes standard short text fields (trim, lowercase).
     */
    public String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    /**
     * Normalizes customer feedback text for deduplication without altering stored text.
     * Trims leading/trailing whitespace, converts to lowercase, and collapses internal consecutive whitespace.
     */
    public String normalizeFeedbackText(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in environment", e);
        }
    }
}
