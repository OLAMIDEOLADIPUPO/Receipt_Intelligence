package com.olamide.receipthandler.service.serviceImpl;


import com.olamide.receipthandler.dto.AuthResponse;
import com.olamide.receipthandler.dto.LoginRequest;
import com.olamide.receipthandler.dto.RegisterRequest;
import com.olamide.receipthandler.exceptions.InvalidFileException;
import com.olamide.receipthandler.models.User;
import com.olamide.receipthandler.repository.UserRepository;
import com.olamide.receipthandler.security.JwtUtil;
import com.olamide.receipthandler.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidFileException("Email already registered.");
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidFileException("User not found."));
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }
}
