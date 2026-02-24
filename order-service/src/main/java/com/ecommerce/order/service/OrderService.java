package com.ecommerce.order.service;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url:http://product-service:8081}")
    private String productServiceUrl;

    public List<Order> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomer(String email) {
        log.info("Fetching orders for customer: {}", email);
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(email);
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        log.info("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public Order createOrder(Order order) {
        log.info("Creating order for customer: {}, productId: {}", order.getCustomerEmail(), order.getProductId());
        order.setStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());
        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        log.info("Updating order {} status to {}", id, newStatus);
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(newStatus);
                    return orderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Transactional
    public Order cancelOrder(Long id) {
        log.info("Cancelling order: {}", id);
        return orderRepository.findById(id)
                .map(order -> {
                    if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                        order.getStatus() == Order.OrderStatus.DELIVERED) {
                        throw new RuntimeException("Cannot cancel order that is already " + order.getStatus());
                    }
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    return orderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
}
