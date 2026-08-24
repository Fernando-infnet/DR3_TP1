package com.marketflow.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketflow.orderservice.client.ProductClient;
import com.marketflow.orderservice.client.dto.ProductResponse;
import com.marketflow.orderservice.domain.Order;
import com.marketflow.orderservice.domain.OrderStatus;
import com.marketflow.orderservice.exception.ResourceNotFoundException;
import com.marketflow.orderservice.repository.OrderRepository;
import com.marketflow.orderservice.web.dto.CreateOrderRequest;
import com.marketflow.orderservice.web.dto.OrderItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_whenProductServiceAvailable_confirmsOrder() {
        ProductResponse product = new ProductResponse("p1", "Teclado mecânico", "ABNT2",
                new BigDecimal("349.90"), 12, "c1");
        when(productClient.getProduct("p1")).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest("cliente-123",
                List.of(new OrderItemRequest("p1", 2)));

        Order result = orderService.createOrder(request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("699.80");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Teclado mecânico");
    }

    @Test
    void createOrder_whenProductServiceUnavailable_fallsBackToAwaitingValidation() {
        // productClient retornando null simula o fallback (ProductClientFallback) acionado
        // pelo circuit breaker quando product-service está fora do ar.
        when(productClient.getProduct("p1")).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest("cliente-456",
                List.of(new OrderItemRequest("p1", 1)));

        Order result = orderService.createOrder(request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.AWAITING_VALIDATION);
        assertThat(result.getTotalAmount()).isNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void createOrder_persistsOrderWithComputedTotal() {
        ProductResponse product = new ProductResponse("p1", "Mouse", "óptico",
                new BigDecimal("50.00"), 100, "c1");
        when(productClient.getProduct("p1")).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(new CreateOrderRequest("cliente-789",
                List.of(new OrderItemRequest("p1", 3))));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void getById_whenExists_returnsOrder() {
        Order order = new Order();
        order.setId("o1");
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        Order result = orderService.getById("o1");

        assertThat(result.getId()).isEqualTo("o1");
    }

    @Test
    void getById_whenMissing_throwsResourceNotFound() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_setsStatusToCancelled() {
        Order order = new Order();
        order.setId("o1");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancel("o1");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
