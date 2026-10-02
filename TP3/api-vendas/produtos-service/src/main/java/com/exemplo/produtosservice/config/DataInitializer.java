package com.exemplo.produtosservice.config;

import com.exemplo.produtosservice.model.Produto;
import com.exemplo.produtosservice.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

/**
 * Popula o banco com produtos de teste na inicializacao, se a tabela estiver vazia.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ProdutoRepository produtoRepository;

    public DataInitializer(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Override
    public void run(String... args) {
        Flux<Produto> produtos = Flux.just(
                new Produto("Notebook", new BigDecimal("3500.00")),
                new Produto("Mouse sem fio", new BigDecimal("79.90")),
                new Produto("Teclado mecanico", new BigDecimal("299.90")),
                new Produto("Monitor 27 polegadas", new BigDecimal("1299.00")),
                new Produto("Webcam Full HD", new BigDecimal("199.90")),
                new Produto("Headset gamer", new BigDecimal("249.50")),
                new Produto("SSD 1TB", new BigDecimal("459.90")),
                new Produto("Cadeira de escritorio", new BigDecimal("899.00")),
                new Produto("Carregador USB-C 65W", new BigDecimal("129.90")),
                new Produto("Smartphone", new BigDecimal("2199.00")));

        // .block() apenas aqui, na inicializacao, para garantir os dados antes de receber requisicoes
        produtoRepository.count()
                .filter(total -> total == 0)
                .flatMapMany(vazio -> produtoRepository.saveAll(produtos))
                .then()
                .block();
    }
}
