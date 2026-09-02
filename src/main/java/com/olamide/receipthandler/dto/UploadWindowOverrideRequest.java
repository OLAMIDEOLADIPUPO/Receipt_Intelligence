package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.OverrideMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UploadWindowOverrideRequest(
        @Schema(description = "AUTO resumes the default 10th-15th schedule; FORCE_OPEN keeps it open "
                + "regardless of date; FORCE_CLOSED keeps it closed regardless of date.")
        @NotNull(message = "mode is required.")
        OverrideMode mode
) {}
