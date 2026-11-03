package com.customerfeedback.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SemanticSearchRequest(
        @NotBlank @Size(min = 2, max = 2000) String query,
        @Min(1) @Max(50) Integer limit
) {}
