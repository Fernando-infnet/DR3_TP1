package com.exemplo.vendasservice.dto;

import java.math.BigDecimal;

/** Produto retornado pelo produtos-service (GET /produtos/{id}). */
public record ProdutoResponse(Long id, String nome, BigDecimal preco) {
}
