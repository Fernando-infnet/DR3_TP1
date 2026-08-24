package com.marketflow.productservice.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.productservice.domain.Product;
import com.marketflow.productservice.repository.ProductRepository;
import com.marketflow.productservice.web.dto.ProductRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductRepository productRepository;

    private Product sampleProduct() {
        Product product = new Product();
        product.setId("p1");
        product.setName("Teclado mecânico");
        product.setDescription("ABNT2");
        product.setPrice(new BigDecimal("349.90"));
        product.setStock(12);
        product.setCategoryId("c1");
        return product;
    }

    @Test
    void listAll_returnsAllProducts() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Teclado mecânico"));
    }

    @Test
    void getById_whenExists_returnsProduct() throws Exception {
        when(productRepository.findById("p1")).thenReturn(Optional.of(sampleProduct()));

        mockMvc.perform(get("/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.price").value(349.90));
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_withValidPayload_returns201() throws Exception {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct());
        ProductRequest request = new ProductRequest("Teclado mecânico", "ABNT2",
                new BigDecimal("349.90"), 12, "c1");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("p1"));
    }

    @Test
    void create_withInvalidPayload_returns400() throws Exception {
        // price negativo viola @PositiveOrZero
        ProductRequest request = new ProductRequest("Teclado", "desc", new BigDecimal("-1"), 1, "c1");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_whenExists_returns204() throws Exception {
        when(productRepository.existsById("p1")).thenReturn(true);

        mockMvc.perform(delete("/products/p1"))
                .andExpect(status().isNoContent());

        verify(productRepository, times(1)).deleteById(eq("p1"));
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        when(productRepository.existsById("missing")).thenReturn(false);

        mockMvc.perform(delete("/products/missing"))
                .andExpect(status().isNotFound());
    }
}
