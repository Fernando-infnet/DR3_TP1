package com.exemplo.vendasservice.repository;

import com.exemplo.vendasservice.TestcontainersConfiguration;
import com.exemplo.vendasservice.model.Venda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a camada de persistencia REATIVA (Spring Data R2DBC) contra um PostgreSQL real (Testcontainers).
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class VendaRepositoryTest {

    @Autowired
    private VendaRepository vendaRepository;

    @BeforeEach
    void limpar() {
        vendaRepository.deleteAll().block();
    }

    @Test
    void deveSalvarVendaComTodasAsColunas() {
        StepVerifier.create(vendaRepository.save(new Venda(7L, 3, new BigDecimal("79.90"), "maria"))
                        .flatMap(v -> vendaRepository.findById(v.getIdVenda())))
                .assertNext(v -> {
                    assertThat(v.getIdVenda()).isNotNull();
                    assertThat(v.getIdProduto()).isEqualTo(7L);
                    assertThat(v.getQuantidade()).isEqualTo(3);
                    assertThat(v.getValorProduto()).isEqualByComparingTo("79.90");
                    assertThat(v.getValorTotal()).isEqualByComparingTo("239.70");
                    assertThat(v.getUsuario()).isEqualTo("maria");
                    assertThat(v.getDataVenda()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void deveListarApenasVendasDoUsuarioDaMaisRecenteParaAMaisAntiga() {
        Flux<Venda> vendas = vendaRepository.saveAll(Flux.just(
                new Venda(1L, 1, BigDecimal.TEN, "joao"),
                new Venda(2L, 1, BigDecimal.TEN, "ana"),
                new Venda(3L, 1, BigDecimal.TEN, "joao")));

        StepVerifier.create(vendas.thenMany(vendaRepository.findByUsuarioOrderByIdVendaDesc("joao")))
                .assertNext(v -> assertThat(v.getIdProduto()).isEqualTo(3L))
                .assertNext(v -> assertThat(v.getIdProduto()).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void usuarioSemVendasDeveRetornarFluxVazio() {
        StepVerifier.create(vendaRepository.findByUsuarioOrderByIdVendaDesc("ninguem"))
                .verifyComplete();
    }

    @Test
    void bancoDeveRejeitarQuantidadeInvalida() {
        Venda invalida = new Venda(1L, 1, BigDecimal.TEN, "joao");
        invalida.setQuantidade(0);

        StepVerifier.create(vendaRepository.save(invalida))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
