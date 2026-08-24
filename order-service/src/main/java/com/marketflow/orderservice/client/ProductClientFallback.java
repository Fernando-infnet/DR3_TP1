package com.marketflow.orderservice.client;

import com.marketflow.orderservice.client.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Acionado quando product-service está indisponível, lento (timeout) ou o circuito
 * está aberto. Retorna null em vez de propagar o erro — o OrderService interpreta
 * isso como "catálogo indisponível" e cria o pedido como AWAITING_VALIDATION.
 */
@Component
public class ProductClientFallback implements ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallback.class);

    @Override
    public ProductResponse getProduct(String id) {
        log.warn("Fallback acionado: product-service indisponível ao consultar produto {}", id);
        return null;
    }
}
