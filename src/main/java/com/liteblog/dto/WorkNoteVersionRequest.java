package com.liteblog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WorkNoteVersionRequest(@NotNull @Min(0) Long version) {
}
