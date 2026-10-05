package org.example.dto;

import org.example.model.Role;
import org.example.model.User;

public record UserResponse(Long id, String username, Role role, boolean enabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled());
    }

}
