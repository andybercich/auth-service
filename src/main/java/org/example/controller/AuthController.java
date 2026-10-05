package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login attempt: username={}", request.username());

        LoginResponse response = authService.login(request);

        log.info("Login successful: username={}", request.username());

        return ResponseEntity.ok(response);
    }
}
