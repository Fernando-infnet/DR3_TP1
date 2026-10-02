package com.exemplo.produtosservice.repository;

import com.exemplo.produtosservice.model.Produto;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Repositorio reativo (R2DBC): todos os metodos retornam Mono/Flux, sem bloquear threads. */
@Repository
public interface ProdutoRepository extends ReactiveCrudRepository<Produto, Long> {

    Flux<Produto> findByNomeContainingIgnoreCase(String nome);
}
