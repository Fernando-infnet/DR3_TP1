package com.exemplo.vendasservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NovaVendaRequest(@NotNull Long idProduto, @NotNull @Min(1) Integer quantidade) {
}
