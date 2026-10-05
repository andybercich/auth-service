package org.example.dto;

import org.example.model.Role;

public record LoginResponse(
        String token,
        String username,

        Role role
) {}
