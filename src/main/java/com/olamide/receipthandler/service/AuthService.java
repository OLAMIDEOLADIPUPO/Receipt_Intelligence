package com.olamide.receipthandler.service;


import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
