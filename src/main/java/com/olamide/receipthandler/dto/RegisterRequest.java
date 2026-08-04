package com.olamide.receipthandler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New-account registration details")
public record RegisterRequest(

        @Schema(description = "Email address; must be unique", example = "yesirat@example.com")
        @NotBlank(message = "Email is required.")
        @Email(message = "Please provide a valid email address.")
        String email,

        @Schema(description = "Password, at least 8 characters", example = "s3curePass!", minLength = 8)
        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters long.")
        String password,

        @Schema(description = "User's full name (2–100 characters)", example = "Yesirat Bello")
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
        String fullName

) {}