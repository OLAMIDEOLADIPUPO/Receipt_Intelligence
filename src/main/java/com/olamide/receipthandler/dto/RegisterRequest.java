package com.olamide.receipthandler.dto;

public record RegisterRequest(
        String email,
        String password,
        String fullName
) {}

