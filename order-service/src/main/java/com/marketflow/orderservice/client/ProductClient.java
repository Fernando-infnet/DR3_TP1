package com.marketflow.orderservice.client;

import com.marketflow.orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * "product-service" é resolvido pelo Eureka (mesmo valor de spring.application.name
 * do product-service) — sem host/porta fixos.
 */
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable("id") String id);
}
