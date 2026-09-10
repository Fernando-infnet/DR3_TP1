package com.marketflow.orderservice.client;

import com.marketflow.orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Localmente, "product-service" é resolvido pelo Eureka. Em Docker/Kubernetes,
 * PRODUCT_SERVICE_URL aponta para o nome DNS do serviço, nunca para localhost.
 */
@FeignClient(name = "product-service", url = "${product.service.url:}", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable("id") String id);
}
