package com.exemplo.produtosservice.controller;

import com.exemplo.produtosservice.model.Produto;
import com.exemplo.produtosservice.repository.ProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/** Controller reativo (WebFlux): retorna Mono/Flux e nunca bloqueia a thread do event loop. */
@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoRepository produtoRepository;

    public ProdutoController(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @GetMapping
    public Flux<Produto> listar(@RequestParam(required = false) String nome) {
        return nome == null
                ? produtoRepository.findAll()
                : produtoRepository.findByNomeContainingIgnoreCase(nome);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Produto>> buscarPorId(@PathVariable Long id) {
        return produtoRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** Restrito ao perfil ADMIN (ver SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Produto> criar(@Valid @RequestBody NovoProduto request) {
        return produtoRepository.save(new Produto(request.nome(), request.preco()));
    }

    /** Mostra quem esta autenticado, a partir dos claims do JWT recebido. */
    @GetMapping("/me")
    public Mono<Map<String, Object>> usuarioAutenticado(@AuthenticationPrincipal Jwt jwt) {
        return Mono.just(Map.of(
                "usuario", jwt.getSubject(),
                "roles", jwt.getClaimAsStringList("roles"),
                "expiraEm", jwt.getExpiresAt()));
    }

    public record NovoProduto(@NotBlank String nome, @NotNull @DecimalMin("0.01") BigDecimal preco) {
    }
}
