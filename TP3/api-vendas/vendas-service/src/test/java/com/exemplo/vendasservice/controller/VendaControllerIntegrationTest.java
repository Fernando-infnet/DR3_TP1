package com.exemplo.vendasservice.controller;

import com.exemplo.vendasservice.TestcontainersConfiguration;
import com.exemplo.vendasservice.model.Venda;
import com.exemplo.vendasservice.repository.VendaRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.exemplo.vendasservice.JwtTestUtils.accessToken;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integracao do fluxo REATIVO completo do vendas-service:
 * WebTestClient -> WebFlux + Security (JWT) -> WebClient (produtos-service) -> R2DBC (PostgreSQL).
 * <p>
 * O PostgreSQL e real (Testcontainers). O produtos-service e simulado por um MockWebServer,
 * registrado como instancia "produtos-service" no SimpleDiscoveryClient — assim a chamada
 * passa pelo mesmo WebClient com LoadBalancer usado em producao (com o Eureka).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class VendaControllerIntegrationTest {

    private static MockWebServer produtosService;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private VendaRepository vendaRepository;

    @BeforeAll
    static void iniciarProdutosServiceFalso() throws IOException {
        produtosService = new MockWebServer();
        produtosService.start();
    }

    @AfterAll
    static void pararProdutosServiceFalso() throws IOException {
        produtosService.shutdown();
    }

    @DynamicPropertySource
    static void registrarProdutosService(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.produtos-service[0].uri",
                () -> "http://localhost:" + produtosService.getPort());
    }

    @BeforeEach
    void limpar() throws InterruptedException {
        vendaRepository.deleteAll().block();
        // descarta requisicoes de testes anteriores
        while (produtosService.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
        }
    }

    @Test
    void semTokenDeveRetornar401SemChamarProdutosService() {
        int chamadasAntes = produtosService.getRequestCount();

        webTestClient.post().uri("/vendas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":1,\"quantidade\":2}")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(produtosService.getRequestCount()).isEqualTo(chamadasAntes);
    }

    @Test
    void deveRegistrarVendaConsultandoProdutoViaWebClientERepassandoOToken() throws InterruptedException {
        produtosService.enqueue(produtoJson(1, "Notebook", "3500.00"));
        String token = accessToken("usuario", "USER");

        webTestClient.post().uri("/vendas")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":1,\"quantidade\":2}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.idVenda").isNotEmpty()
                .jsonPath("$.idProduto").isEqualTo(1)
                .jsonPath("$.quantidade").isEqualTo(2)
                .jsonPath("$.valorProduto").isEqualTo(3500.00)
                .jsonPath("$.valorTotal").isEqualTo(7000.00)
                .jsonPath("$.usuario").isEqualTo("usuario");

        // O WebClient chamou o produtos-service na rota certa, repassando o MESMO token JWT
        RecordedRequest chamada = produtosService.takeRequest(1, TimeUnit.SECONDS);
        assertThat(chamada).isNotNull();
        assertThat(chamada.getPath()).isEqualTo("/produtos/1");
        assertThat(chamada.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);

        // E a venda foi persistida no PostgreSQL
        StepVerifier.create(vendaRepository.findByUsuarioOrderByIdVendaDesc("usuario"))
                .assertNext(v -> assertThat(v.getValorTotal()).isEqualByComparingTo("7000.00"))
                .verifyComplete();
    }

    @Test
    void produtoInexistenteDeveRetornar422ENaoGravarVenda() {
        produtosService.enqueue(new MockResponse().setResponseCode(404));

        webTestClient.post().uri("/vendas")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":999,\"quantidade\":1}")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.mensagem").isEqualTo("Produto 999 nao encontrado no produtos-service");

        StepVerifier.create(vendaRepository.count()).expectNext(0L).verifyComplete();
    }

    @Test
    void produtosServiceLentoDeveRetornar503PorTimeout() {
        // responde depois do timeout configurado (produtos-service.timeout=1s)
        produtosService.enqueue(produtoJson(1, "Notebook", "3500.00").setBodyDelay(3, TimeUnit.SECONDS));

        webTestClient.post().uri("/vendas")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":1,\"quantidade\":1}")
                .exchange()
                .expectStatus().isEqualTo(503);

        StepVerifier.create(vendaRepository.count()).expectNext(0L).verifyComplete();
    }

    @Test
    void erroNoProdutosServiceDeveRetornar503() {
        produtosService.enqueue(new MockResponse().setResponseCode(500));

        webTestClient.post().uri("/vendas")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":1,\"quantidade\":1}")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void quantidadeInvalidaDeveRetornar400SemChamarProdutosService() {
        int chamadasAntes = produtosService.getRequestCount();

        webTestClient.post().uri("/vendas")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"idProduto\":1,\"quantidade\":0}")
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(produtosService.getRequestCount()).isEqualTo(chamadasAntes);
    }

    @Test
    void deveListarSomenteAsVendasDoUsuarioAutenticado() {
        vendaRepository.save(new Venda(1L, 1, java.math.BigDecimal.TEN, "usuario"))
                .then(vendaRepository.save(new Venda(2L, 1, java.math.BigDecimal.TEN, "admin")))
                .block();

        webTestClient.get().uri("/vendas")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Venda.class).value(vendas -> {
                    assertThat(vendas).hasSize(1);
                    assertThat(vendas.get(0).getUsuario()).isEqualTo("usuario");
                });
    }

    @Test
    void vendaInexistenteDeveRetornar404() {
        webTestClient.get().uri("/vendas/99999")
                .headers(h -> h.setBearerAuth(accessToken("usuario", "USER")))
                .exchange()
                .expectStatus().isNotFound();
    }

    private static MockResponse produtoJson(long id, String nome, String preco) {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"id\":%d,\"nome\":\"%s\",\"preco\":%s}".formatted(id, nome, preco));
    }
}
