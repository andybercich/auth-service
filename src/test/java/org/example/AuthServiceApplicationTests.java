package org.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.CreateUserRequest;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.exception.DuplicateUsernameException;
import org.example.model.Role;
import org.example.repository.UserRepository;
import org.example.security.JwtService;
import org.example.service.AuthService;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceApplicationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserService userService;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        userService.create(new CreateUserRequest("admin", "password123", Role.ADMIN));
    }

    @Test
    void loginCorrectoGeneraJwtConClaimsYExpiration() {
        LoginResponse response = authService.login(new LoginRequest("admin", "password123"));
        var claims = jwtService.parseToken(response.token());
        assertThat(response.token()).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get("userId", Long.class)).isNotNull();
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void passwordIncorrectoDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nadie\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDeshabilitadoNoPuedeIniciarSesion() throws Exception {
        userService.disable(userRepository.findByUsername("admin").orElseThrow().getId());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordSeAlmacenaComoBcrypt() {
        String stored = userRepository.findByUsername("admin").orElseThrow().getPassword();
        assertThat(stored).startsWith("$2").isNotEqualTo("password123");
    }

    @Test
    void usernameDuplicadoSeRechaza() {
        org.junit.jupiter.api.Assertions.assertThrows(DuplicateUsernameException.class, () ->
                userService.create(new CreateUserRequest("admin", "password123", Role.USER)));
    }

    @Test
    void validacionesDeDtoDevuelven400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPuedeCrearUsuarioSinExponerPassword() throws Exception {
        String request = objectMapper.writeValueAsString(new CreateUserRequest("operador", "password123", Role.USER));
        mockMvc.perform(post("/users")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").value("operador"));
    }
}
