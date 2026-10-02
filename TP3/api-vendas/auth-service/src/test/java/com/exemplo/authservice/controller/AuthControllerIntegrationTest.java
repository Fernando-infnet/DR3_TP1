package com.exemplo.authservice.controller;

import com.exemplo.authservice.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao do fluxo de autenticacao (login e refresh) com a aplicacao
 * completa e um PostgreSQL real (Testcontainers). Os usuarios vem do DataInitializer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void loginValidoDeveRetornarAccessERefreshToken() throws Exception {
        JsonNode tokens = login("admin", "admin123");

        Jwt access = jwtDecoder.decode(tokens.get("accessToken").asText());
        assertThat(access.getSubject()).isEqualTo("admin");
        assertThat(access.getClaimAsString("token_type")).isEqualTo("access");
        assertThat(access.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "ADMIN");

        Jwt refresh = jwtDecoder.decode(tokens.get("refreshToken").asText());
        assertThat(refresh.getClaimAsString("token_type")).isEqualTo("refresh");
        assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(300);
    }

    @Test
    void loginComSenhaErradaDeveRetornar401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuario ou senha invalidos"));
    }

    @Test
    void loginComUsuarioInexistenteDeveRetornar401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fantasma\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSemCamposObrigatoriosDeveRetornar400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshValidoDeveEmitirNovoAccessToken() throws Exception {
        JsonNode tokens = login("usuario", "usuario123");
        Thread.sleep(1000); // garante "iat" diferente

        String resposta = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String novoAccess = objectMapper.readTree(resposta).get("accessToken").asText();
        assertThat(novoAccess).isNotEqualTo(tokens.get("accessToken").asText());
        Jwt jwt = jwtDecoder.decode(novoAccess);
        assertThat(jwt.getSubject()).isEqualTo("usuario");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
    }

    @Test
    void refreshComAccessTokenDeveRetornar401() throws Exception {
        JsonNode tokens = login("admin", "admin123");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("accessToken").asText() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("O token informado nao e um refresh token"));
    }

    @Test
    void refreshComTokenInvalidoDeveRetornar401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"token-invalido\"}"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login(String username, String password) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }
}
