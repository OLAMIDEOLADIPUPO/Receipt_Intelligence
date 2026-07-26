package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.configurations.RefreshTokenCookieFactory;
import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;
import com.olamide.receipthandler.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshTokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.authResponse());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                                HttpServletResponse response) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(authHeader, refreshToken);
        response.addCookie(cookieFactory.clear());
        return ResponseEntity.noContent().build();
    }
}