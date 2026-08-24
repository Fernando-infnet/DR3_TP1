package com.marketflow.productservice.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.productservice.domain.Category;
import com.marketflow.productservice.repository.CategoryRepository;
import com.marketflow.productservice.web.dto.CategoryRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @Test
    void listAll_returnsAllCategories() throws Exception {
        Category category = new Category();
        category.setId("c1");
        category.setName("Eletrônicos");
        category.setDescription("Periféricos e acessórios");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Eletrônicos"));
    }

    @Test
    void create_withValidPayload_returns201() throws Exception {
        Category saved = new Category();
        saved.setId("c1");
        saved.setName("Eletrônicos");
        saved.setDescription("Periféricos e acessórios");
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryRequest request = new CategoryRequest("Eletrônicos", "Periféricos e acessórios");

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest("", "sem nome");

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
