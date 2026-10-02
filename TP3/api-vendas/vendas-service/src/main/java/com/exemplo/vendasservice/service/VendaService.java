package com.exemplo.vendasservice.service;

import com.exemplo.vendasservice.client.ProdutoClient;
import com.exemplo.vendasservice.dto.NovaVendaRequest;
import com.exemplo.vendasservice.model.Venda;
import com.exemplo.vendasservice.repository.VendaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class VendaService {

    private final ProdutoClient produtoClient;
    private final VendaRepository vendaRepository;

    public VendaService(ProdutoClient produtoClient, VendaRepository vendaRepository) {
        this.produtoClient = produtoClient;
        this.vendaRepository = vendaRepository;
    }

    /**
     * Fluxo 100% reativo: busca o produto no produtos-service (WebClient) e, com o preco
     * retornado, grava a venda no banco (R2DBC) — sem bloquear nenhuma thread.
     */
    public Mono<Venda> registrar(NovaVendaRequest request, String usuario) {
        return produtoClient.buscarProduto(request.idProduto())
                .map(produto -> new Venda(produto.id(), request.quantidade(), produto.preco(), usuario))
                .flatMap(vendaRepository::save);
    }

    public Flux<Venda> listarDoUsuario(String usuario) {
        return vendaRepository.findByUsuarioOrderByIdVendaDesc(usuario);
    }

    public Mono<Venda> buscarPorId(Long idVenda) {
        return vendaRepository.findById(idVenda);
    }
}
