package com.exemplo.produtosservice.controller;

import com.exemplo.produtosservice.TestcontainersConfiguration;
import com.exemplo.produtosservice.model.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.exemplo.produtosservice.JwtTestUtils.accessToken;
import static com.exemplo.produtosservice.JwtTestUtils.refreshToken;
import static com.exemplo.produtosservice.JwtTestUtils.tokenDeOutroEmissor;
import static com.exemplo.produtosservice.JwtTestUtils.tokenExpirado;

/**
 * Teste de integracao da aplicacao REATIVA completa (Netty + WebFlux + Security + R2DBC)
 * com um PostgreSQL real (Testcontainers), usando o WebTestClient.
 * Os 10 produtos iniciais sao criados pelo DataInitializer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ProdutoControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void semTokenDeveRetornar401() {
        webTestClient.get().uri("/produtos")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody().jsonPath("$.mensagem").isEqualTo("Token de acesso ausente, invalido ou expirado");
    }

    @Test
    void comTokenValidoDeveListarProdutos() {
        webTestClient.get().uri("/produtos")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Produto.class).value(lista ->
                        org.assertj.core.api.Assertions.assertThat(lista).hasSizeGreaterThanOrEqualTo(10));
    }

    @Test
    void deveBuscarProdutoPorId() {
        webTestClient.get().uri("/produtos/1")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.nome").isEqualTo("Notebook");
    }

    @Test
    void produtoInexistenteDeveRetornar404() {
        webTestClient.get().uri("/produtos/99999")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void tokenAdulteradoDeveRetornar401() {
        webTestClient.get().uri("/produtos")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER") + "x"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenExpiradoDeveRetornar401() {
        webTestClient.get().uri("/produtos")
                .headers(h -> h.setBearerAuth(tokenExpirado("usuario", "USER")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void tokenDeOutroEmissorDeveRetornar401() {
        webTestClient.get().uri("/produtos")
                .headers(h -> h.setBearerAuth(tokenDeOutroEmissor("usuario", "USER")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refreshTokenNaoDeveSerAceitoComoAccessToken() {
        webTestClient.get().uri("/produtos")
                .headers(h -> h.setBearerAuth(refreshToken("usuario")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void usuarioSemPerfilAdminNaoPodeCriarProduto() {
        webTestClient.post().uri("/produtos")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"Pendrive\",\"preco\":39.90}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminDeveCriarProdutoEPersistirNoBanco() {
        String token = accessToken("admin", "USER", "ADMIN");

        Produto criado = webTestClient.post().uri("/produtos")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"Pendrive\",\"preco\":39.90}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Produto.class).returnResult().getResponseBody();

        webTestClient.get().uri("/produtos/{id}", criado.getId())
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.nome").isEqualTo("Pendrive");
    }

    @Test
    void criarProdutoInvalidoDeveRetornar400() {
        webTestClient.post().uri("/produtos")
                .headers(h -> h.setBearerAuth(accessToken("admin", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"\",\"preco\":-1}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void meDeveRetornarDadosDoToken() {
        webTestClient.get().uri("/produtos/me")
                .headers(h -> h.setBearerAuth(accessToken("admin", "USER", "ADMIN")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuario").isEqualTo("admin")
                .jsonPath("$.roles.length()").isEqualTo(2);
    }
}
