package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.model.Role;

public record CreateUserRequest(
        @NotBlank(message = "username es obligatorio")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "password es obligatorio")
        @Size(min = 8, max = 72, message = "password debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "role es obligatorio")
        Role role

) {
}
