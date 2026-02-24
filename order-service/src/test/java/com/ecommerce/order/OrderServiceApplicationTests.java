package com.ecommerce.order;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Order order = Order.builder()
                .productId(1L)
                .quantity(2)
                .totalAmount(new BigDecimal("199.98"))
                .customerEmail("test@example.com")
                .customerName("John Doe")
                .shippingAddress("123 Main St, City, Country")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerEmail").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404ForNonExistentOrder() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/orders/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Order Service is UP"));
    }
}
