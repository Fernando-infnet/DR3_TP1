package com.marketflow.orderservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotBlank String productId,
        @Positive Integer quantity
) {
}
