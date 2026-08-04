package com.olamide.receipthandler.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful authentication result. The refresh token is returned separately as an HttpOnly cookie.")
public record AuthResponse(
        @Schema(description = "JWT access token to send as `Authorization: Bearer <token>`",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ5ZXNpcmF0In0...")
        String token,
        @Schema(description = "Authenticated user's email", example = "yesirat@example.com")
        String email,
        @Schema(description = "Authenticated user's full name", example = "Yesirat Bello")
        String fullName
) {}