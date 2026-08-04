package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.dto.StaffResponseDTO;
import com.olamide.receipthandler.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "Look up and create staff members to tag batch-uploaded receipts against. Requires a Bearer access token.")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @Operation(summary = "Search staff",
            description = "Returns staff members matching the query (typeahead). With no query, returns all staff.")
    @ApiResponse(responseCode = "200", description = "Matching staff returned")
    public ResponseEntity<List<StaffResponseDTO>> search(
            @Parameter(description = "Case-insensitive name fragment to match; omit to list everyone")
            @RequestParam(required = false) String query) {
        List<StaffResponseDTO> staff = staffService.search(query).stream()
                .map(StaffResponseDTO::from)
                .toList();
        return ResponseEntity.ok(staff);
    }

    // New: called when the typed name doesn't match an existing staff member.
    @PostMapping
    @Operation(summary = "Find or create a staff member",
            description = "Returns the staff member with the given name, creating them if none exists. "
                    + "Idempotent by name — safe to call when a typed name doesn't match an existing member.")
    @ApiResponse(responseCode = "200", description = "Existing or newly created staff member returned")
    public ResponseEntity<StaffResponseDTO> create(@RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(StaffResponseDTO.from(staffService.findOrCreate(request.name())));
    }

    @Schema(description = "Request to find or create a staff member by name")
    public record CreateStaffRequest(
            @Schema(description = "Full name of the staff member", example = "Yesirat Bello")
            String name) {}
}