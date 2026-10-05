package org.example.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.example.dto.CreateUserRequest;
import org.example.dto.UserResponse;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("auth/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {

        log.info("Creating user");

        UserResponse response = userService.create(request);

        log.info("User created successfully: userId={}", response.id());

        return ResponseEntity.created(URI.create("/users/" + response.id())).body(response);
    }

    @GetMapping
    public List<UserResponse> findAll() {

        log.debug("Fetching all users");

        return userService.findAll();
    }

    @GetMapping("/me")
    public UserResponse findeUsername(Authentication authentication) {

        log.debug(
                "Fetching authenticated user: username={}",
                authentication.getName()
        );

        return userService.findResponseByUsername(
                authentication.getName()
        );
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {

        log.debug("Fetching user: userId={}", id);

        return userService.findById(id);
    }

    @DeleteMapping("/{id}")
    public UserResponse disable(@PathVariable Long id) {

        log.info("Disabling user: userId={}", id);

        UserResponse response = userService.disable(id);

        log.info("User disabled successfully: userId={}", id);

        return response;
    }
}
