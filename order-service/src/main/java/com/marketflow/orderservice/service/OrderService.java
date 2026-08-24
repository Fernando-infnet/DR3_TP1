package com.marketflow.orderservice.service;

import com.marketflow.orderservice.client.ProductClient;
import com.marketflow.orderservice.client.dto.ProductResponse;
import com.marketflow.orderservice.domain.Order;
import com.marketflow.orderservice.domain.OrderItem;
import com.marketflow.orderservice.domain.OrderStatus;
import com.marketflow.orderservice.exception.ResourceNotFoundException;
import com.marketflow.orderservice.repository.OrderRepository;
import com.marketflow.orderservice.web.dto.CreateOrderRequest;
import com.marketflow.orderservice.web.dto.OrderItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());

        BigDecimal total = BigDecimal.ZERO;
        boolean catalogUnavailable = false;

        for (OrderItemRequest itemRequest : request.items()) {
            // Chamada protegida por circuit breaker + timeout + fallback
            // (ver ProductClient / ProductClientFallback e config de resilience4j).
            ProductResponse product = productClient.getProduct(itemRequest.productId());

            if (product == null) {
                catalogUnavailable = true;
                log.warn("product-service indisponível ao validar produto {}", itemRequest.productId());
                continue;
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.id());
            item.setProductName(product.name());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.price());
            order.getItems().add(item);

            total = total.add(product.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        if (catalogUnavailable) {
            order.setStatus(OrderStatus.AWAITING_VALIDATION);
            order.setTotalAmount(null);
        } else {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setTotalAmount(total);
        }

        return orderRepository.save(order);
    }

    public Order getById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
    }

    public List<Order> listByCustomer(String customerId) {
        return customerId == null ? orderRepository.findAll() : orderRepository.findByCustomerId(customerId);
    }

    public Order cancel(String id) {
        Order order = getById(id);
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
