package com.exemplo.vendasservice.controller;

import com.exemplo.vendasservice.dto.NovaVendaRequest;
import com.exemplo.vendasservice.model.Venda;
import com.exemplo.vendasservice.service.VendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Venda> registrar(@Valid @RequestBody NovaVendaRequest request, @AuthenticationPrincipal Jwt jwt) {
        return vendaService.registrar(request, jwt.getSubject());
    }

    /** Vendas registradas pelo usuario autenticado. */
    @GetMapping
    public Flux<Venda> minhasVendas(@AuthenticationPrincipal Jwt jwt) {
        return vendaService.listarDoUsuario(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Venda>> buscarPorId(@PathVariable Long id) {
        return vendaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
