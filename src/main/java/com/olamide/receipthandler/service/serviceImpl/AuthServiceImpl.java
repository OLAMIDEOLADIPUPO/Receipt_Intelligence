package com.olamide.receipthandler.service.serviceImpl;

import com.olamide.receipthandler.configurations.JwtService;
import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;
import com.olamide.receipthandler.exceptions.EmailAlreadyExistsException;
import com.olamide.receipthandler.exceptions.InvalidCredentialsException;
import com.olamide.receipthandler.exceptions.InvalidTokenException;
import com.olamide.receipthandler.models.RefreshTokens;
import com.olamide.receipthandler.models.RevokedToken;
import com.olamide.receipthandler.models.User;
import com.olamide.receipthandler.repository.RefreshTokenRepository;
import com.olamide.receipthandler.repository.RevokedTokenRepository;
import com.olamide.receipthandler.repository.UserRepository;
import com.olamide.receipthandler.service.AuthService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RevokedTokenRepository revokedTokenRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email already exists.");
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        userRepository.save(user);
        return issueTokenPair(user);
    }

    @Override
    public AuthResult login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(), request.password()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Incorrect email or password.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Incorrect email or password."));
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Missing refresh token.");
        }
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token.");
        }

        String jti = jwtService.extractJti(refreshToken);
        RefreshTokens stored = refreshTokenRepository.findByJtiForUpdate(jti)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not recognized. Please log in again."));
        if (stored.isRevoked()) {
            // reuse of an already-rotated-out token — treat as theft, burn the whole chain
            refreshTokenRepository.revokeAllActiveTokensForUser(stored.getUser());
            throw new InvalidTokenException("This session is no longer valid. Please log in again.");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists."));

        stored.revoke();

        AuthResult newTokens = issueTokenPair(user);
        String newRefreshJti = jwtService.extractJti(newTokens.refreshToken());
        stored.setReplacedByJti(newRefreshJti);
        refreshTokenRepository.save(stored);

        return newTokens;
    }

    @Override
    public void logout(String authHeader, String refreshToken) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                String jti = jwtService.extractJti(accessToken);
                Instant expiresAt = jwtService.extractExpiration(accessToken).toInstant();
                revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
            } catch (JwtException e) {
                // Token is malformed/expired/tampered — nothing to revoke, and the
                // client is logging out either way. Log for visibility, don't block
                // the rest of logout (the refresh token below still needs handling).
                logger.debug("Could not parse access token during logout: {}", e.getMessage());
            }
        }
        if (refreshToken != null) {
            try {
                String refreshJti = jwtService.extractJti(refreshToken);
                refreshTokenRepository.findByJti(refreshJti).ifPresent(RefreshTokens::revoke);
            } catch (JwtException e) {
                logger.debug("Could not parse refresh token during logout: {}", e.getMessage());
            }
        }
    }

    private AuthResult issueTokenPair(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String refreshJti = jwtService.extractJti(refreshToken);
        Instant refreshExpiresAt = jwtService.extractExpiration(refreshToken).toInstant();
        refreshTokenRepository.save(new RefreshTokens(refreshJti, user, refreshExpiresAt));

        AuthResponse authResponse = new AuthResponse(accessToken, user.getEmail(), user.getFullName());
        return new AuthResult(authResponse, refreshToken);
    }
}