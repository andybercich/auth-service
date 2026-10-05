package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username es obligatorio") @Size(max = 50)
        String username,
        @NotBlank(message = "password es obligatorio") @Size(max = 72)
        String password
) {
}
