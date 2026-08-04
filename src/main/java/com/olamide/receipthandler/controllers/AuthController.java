package com.olamide.receipthandler.controllers;

import com.olamide.receipthandler.configurations.RefreshTokenCookieFactory;
import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;
import com.olamide.receipthandler.exceptions.ErrorResponse;
import com.olamide.receipthandler.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh, and logout. These endpoints are public — no access token required.")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshTokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates an account and immediately signs the user in. Returns an access token "
                    + "in the body and sets a refresh token as an HttpOnly cookie.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; access token returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed (invalid email, weak password, missing name)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result.authResponse());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = "Authenticates an existing user. Returns an access token in the body and sets a "
                    + "refresh token as an HttpOnly cookie.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated; access token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh the access token",
            description = "Exchanges the refresh token (sent automatically as an HttpOnly cookie) for a new "
                    + "access token and a rotated refresh token. No request body is needed.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "401", description = "Missing, expired, or invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                                HttpServletResponse response) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        response.addCookie(cookieFactory.create(result.refreshToken()));
        return ResponseEntity.ok(result.authResponse());
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out",
            description = "Revokes the current access token and clears the refresh-token cookie. Safe to call "
                    + "with or without a valid token; always succeeds.",
            security = {})
    @ApiResponse(responseCode = "204", description = "Logged out; refresh cookie cleared")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(authHeader, refreshToken);
        response.addCookie(cookieFactory.clear());
        return ResponseEntity.noContent().build();
    }
}