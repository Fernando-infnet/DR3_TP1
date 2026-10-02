package com.exemplo.vendasservice.repository;

import com.exemplo.vendasservice.model.Venda;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Repositorio reativo (R2DBC). */
@Repository
public interface VendaRepository extends ReactiveCrudRepository<Venda, Long> {

    Flux<Venda> findByUsuarioOrderByIdVendaDesc(String usuario);
}
