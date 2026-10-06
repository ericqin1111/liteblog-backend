package com.liteblog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

public record WorkNoteSaveRequest(
        @NotNull @Size(max = 200) String title,
        @NotNull @Size(max = 2000) String mainProblem,
        @NotNull JsonNode content,
        @NotNull Boolean archived,
        @NotNull @Min(0) Long version) {
}
