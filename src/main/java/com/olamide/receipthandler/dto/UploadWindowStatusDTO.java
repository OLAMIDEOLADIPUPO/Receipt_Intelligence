package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.enums.OverrideMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the public staff self-upload page is currently accepting submissions.")
public record UploadWindowStatusDTO(
        @Schema(description = "True if submissions are accepted right now") boolean open,
        @Schema(description = "AUTO follows the default schedule; FORCE_OPEN/FORCE_CLOSED override it") OverrideMode overrideMode,
        @Schema(description = "Day of month the automatic window opens", example = "10") int autoWindowStartDay,
        @Schema(description = "Day of month the automatic window closes (inclusive)", example = "15") int autoWindowEndDay
) {}
