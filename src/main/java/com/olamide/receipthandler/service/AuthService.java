package com.olamide.receipthandler.service;

import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;

public interface AuthService {

    record AuthResult(AuthResponse authResponse, String refreshToken) {}

    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
    AuthResult refresh(String refreshToken);
    void logout(String authHeader, String refreshToken);
}