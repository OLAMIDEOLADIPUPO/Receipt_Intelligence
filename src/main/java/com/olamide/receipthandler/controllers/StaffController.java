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
@Tag(name = "Staff", description = "Manage the staff roster (name + employeeId). Requires a Bearer access token.")
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

    // Called by Accounts to add someone to the roster — this is what makes an
    // employeeId valid for the public self-upload flow to accept later.
    @PostMapping
    @Operation(summary = "Find or create a staff member",
            description = "Returns the staff member with the given employeeId, creating them if none exists. "
                    + "Idempotent by employeeId (the roster key) — safe to call again with the same ID; "
                    + "the existing record is returned unchanged even if the name submitted differs.")
    @ApiResponse(responseCode = "200", description = "Existing or newly created staff member returned")
    public ResponseEntity<StaffResponseDTO> create(@RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(StaffResponseDTO.from(
                staffService.findOrCreate(request.name(), request.employeeId())));
    }

    @Schema(description = "Request to find or create a staff member by name and employee ID")
    public record CreateStaffRequest(
            @Schema(description = "Full name of the staff member", example = "Yesirat Bello")
            String name,
            @Schema(description = "Unique employee ID — the roster key staff will later use to self-identify on upload", example = "REM-0142")
            String employeeId) {}
}