package com.olamide.receipthandler.dto;

public record LoginRequest(
        String email,
        String password
) {}