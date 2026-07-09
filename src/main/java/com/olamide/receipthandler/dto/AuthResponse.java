package com.olamide.receipthandler.dto;

public record AuthResponse(
        String token,
        String email,
        String fullName
) {}