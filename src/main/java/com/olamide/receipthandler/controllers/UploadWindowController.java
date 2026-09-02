package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.dto.UploadWindowOverrideRequest;
import com.olamide.receipthandler.dto.UploadWindowStatusDTO;
import com.olamide.receipthandler.service.UploadWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/upload-window")
@Tag(name = "Upload Window", description = "Controls whether the public staff self-upload page is currently "
        + "accepting submissions. Defaults to an automatic 10th-15th-of-the-month schedule; Accounts can "
        + "override it to close early, pause, or keep it open past the default close date.")
public class UploadWindowController {

    private final UploadWindowService uploadWindowService;

    public UploadWindowController(UploadWindowService uploadWindowService) {
        this.uploadWindowService = uploadWindowService;
    }

    @GetMapping
    @Operation(
            summary = "Get the current upload window status",
            description = "Public — the /upload page checks this before showing the form, so staff see "
                    + "'not open right now' instead of a form that will reject them on submit.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Current status returned")
    public ResponseEntity<UploadWindowStatusDTO> getStatus() {
        return ResponseEntity.ok(uploadWindowService.getStatus());
    }

    @PutMapping
    @Operation(
            summary = "Set the manual override",
            description = "Requires a Bearer token (Accounts). AUTO resumes the default 10th-15th schedule; "
                    + "FORCE_OPEN keeps the window open regardless of date; FORCE_CLOSED keeps it closed "
                    + "regardless of date (close early / pause).")
    @ApiResponse(responseCode = "200", description = "Override applied; current status returned")
    public ResponseEntity<UploadWindowStatusDTO> setOverride(@Valid @RequestBody UploadWindowOverrideRequest request) {
        return ResponseEntity.ok(uploadWindowService.setOverride(request.mode()));
    }
}
