package org.example.service;

import java.util.List;
import org.example.dto.CreateUserRequest;
import org.example.dto.UserResponse;
import org.example.exception.DuplicateUsernameException;
import org.example.exception.UserNotFoundException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {

        String username = request.username().trim();
        log.info("Creating user: username={}", username);

        if (userRepository.existsByUsername(username)) {
            log.warn("User creation failed: username already exists, username={}", username);
            throw new DuplicateUsernameException(username);
        }

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        log.info("User created successfully: userId={}, username={}, role={}", savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole()
        );

        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public UserResponse findResponseByUsername(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {

        log.debug("Fetching user: userId={}", id);

        return UserResponse.from(userRepository.findById(id).orElseThrow(UserNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {

        log.debug("Fetching all users");

        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse disable(Long id) {
        log.info("Disabling user: userId={}", id);

        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setEnabled(false);

        log.info("User disabled successfully: userId={}, username={}", user.getId(), user.getUsername());
        return UserResponse.from(user);
    }
}
