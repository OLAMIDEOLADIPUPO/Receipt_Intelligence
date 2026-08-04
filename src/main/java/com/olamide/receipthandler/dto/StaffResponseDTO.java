package com.olamide.receipthandler.dto;

import com.olamide.receipthandler.models.Staff;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A staff member that receipts can be tagged against")
public record StaffResponseDTO(
        @Schema(description = "Staff ID") UUID id,
        @Schema(description = "Staff member's full name", example = "Yesirat Bello") String name,
        @Schema(description = "Whether the staff member is active", example = "true") boolean active
) {
    public static StaffResponseDTO from(Staff staff) {
        return new StaffResponseDTO(staff.getId(), staff.getName(), staff.isActive());
    }
}
