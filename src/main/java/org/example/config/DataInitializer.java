package org.example.config;

import org.example.dto.CreateUserRequest;
import org.example.model.Role;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean CommandLineRunner bootstrapAdmin(
            UserRepository repository,
            UserService userService,
            @Value("${auth.bootstrap.admin-username:}")
            String username,
            @Value("${auth.bootstrap.admin-password:}") String password) {

        return args -> {
            if (!username.isBlank() && !password.isBlank()
                    && !repository.existsByUsername(username))

                userService.create(new CreateUserRequest(username, password, Role.ADMIN));
        };
    }
}
