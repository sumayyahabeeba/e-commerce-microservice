package com.ecommerce.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/products")
    public ResponseEntity<Map<String, String>> productFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Product Service is temporarily unavailable. Please try again later.",
                        "service", "product-service"
                ));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, String>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Order Service is temporarily unavailable. Please try again later.",
                        "service", "order-service"
                ));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API Gateway is UP");
    }
}
