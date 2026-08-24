package com.marketflow.orderservice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.orderservice.domain.Order;
import com.marketflow.orderservice.domain.OrderStatus;
import com.marketflow.orderservice.exception.ResourceNotFoundException;
import com.marketflow.orderservice.service.OrderService;
import com.marketflow.orderservice.web.dto.CreateOrderRequest;
import com.marketflow.orderservice.web.dto.OrderItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void create_returnsCreatedOrder() throws Exception {
        Order order = new Order();
        order.setId("o1");
        order.setCustomerId("cliente-123");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("699.80"));
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(order);

        CreateOrderRequest request = new CreateOrderRequest("cliente-123",
                List.of(new OrderItemRequest("p1", 2)));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("o1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_withEmptyItems_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("cliente-123", List.of());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        when(orderService.getById("missing"))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado: missing"));

        mockMvc.perform(get("/orders/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_returnsUpdatedOrder() throws Exception {
        Order order = new Order();
        order.setId("o1");
        order.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancel(eq("o1"))).thenReturn(order);

        mockMvc.perform(put("/orders/o1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
