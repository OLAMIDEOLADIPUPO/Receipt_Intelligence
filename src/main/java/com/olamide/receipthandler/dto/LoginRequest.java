package com.olamide.receipthandler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "yesirat@example.com")
        String email,
        @Schema(description = "Account password", example = "s3curePass!")
        String password
) {}