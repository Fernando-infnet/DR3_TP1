package com.exemplo.produtosservice.repository;

import com.exemplo.produtosservice.TestcontainersConfiguration;
import com.exemplo.produtosservice.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a camada de persistencia REATIVA (Spring Data R2DBC) contra um PostgreSQL real
 * (Testcontainers). Os fluxos Mono/Flux sao verificados com o StepVerifier do Reactor.
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void limpar() {
        produtoRepository.deleteAll().block();
    }

    @Test
    void deveSalvarProdutoEGerarId() {
        StepVerifier.create(produtoRepository.save(new Produto("Notebook", new BigDecimal("3500.00"))))
                .assertNext(p -> {
                    assertThat(p.getId()).isNotNull();
                    assertThat(p.getNome()).isEqualTo("Notebook");
                })
                .verifyComplete();
    }

    @Test
    void deveBuscarPorIdComPrecisaoDecimal() {
        Long id = produtoRepository.save(new Produto("Mouse", new BigDecimal("79.90"))).block().getId();

        StepVerifier.create(produtoRepository.findById(id))
                .assertNext(p -> assertThat(p.getPreco()).isEqualByComparingTo("79.90"))
                .verifyComplete();
    }

    @Test
    void buscarIdInexistenteDeveCompletarVazio() {
        StepVerifier.create(produtoRepository.findById(99999L))
                .verifyComplete();
    }

    @Test
    void deveListarTodosComoFlux() {
        Flux<Produto> salvos = produtoRepository.saveAll(Flux.just(
                new Produto("A", BigDecimal.ONE),
                new Produto("B", BigDecimal.TEN),
                new Produto("C", new BigDecimal("2.00"))));

        StepVerifier.create(salvos.thenMany(produtoRepository.findAll()))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void deveBuscarPorNomeIgnorandoMaiusculas() {
        StepVerifier.create(produtoRepository.saveAll(Flux.just(
                                new Produto("Teclado mecanico", BigDecimal.TEN),
                                new Produto("Mouse sem fio", BigDecimal.ONE)))
                        .thenMany(produtoRepository.findByNomeContainingIgnoreCase("TECLADO")))
                .assertNext(p -> assertThat(p.getNome()).isEqualTo("Teclado mecanico"))
                .verifyComplete();
    }

    @Test
    void deveRemoverProduto() {
        StepVerifier.create(produtoRepository.save(new Produto("Remover", BigDecimal.ONE))
                        .flatMap(p -> produtoRepository.deleteById(p.getId()))
                        .then(produtoRepository.count()))
                .expectNext(0L)
                .verifyComplete();
    }
}
