package org.example.service;

import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.exception.InvalidCredentialsException;
import org.example.model.User;
import org.example.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.findByUsername(request.username().trim());
        } catch (RuntimeException exception) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(jwtService.generateToken(user), user.getUsername(), user.getRole());
    }
}