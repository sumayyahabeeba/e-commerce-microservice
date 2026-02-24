package com.ecommerce.product.service;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findByActiveTrue();
    }

    public Optional<Product> getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id);
    }

    public List<Product> searchByName(String name) {
        log.info("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Product> getByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategoryIgnoreCase(category);
    }

    public List<Product> getInStockProducts() {
        return productRepository.findAllInStock();
    }

    @Transactional
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getName());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        log.info("Updating product with id: {}", id);
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setDescription(updated.getDescription());
                    existing.setPrice(updated.getPrice());
                    existing.setStock(updated.getStock());
                    existing.setCategory(updated.getCategory());
                    existing.setActive(updated.getActive());
                    return productRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product); // soft delete
    }

    @Transactional
    public Product updateStock(Long id, Integer quantity) {
        log.info("Updating stock for product id: {}, quantity: {}", id, quantity);
        return productRepository.findById(id)
                .map(product -> {
                    int newStock = product.getStock() + quantity;
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + id);
                    }
                    product.setStock(newStock);
                    return productRepository.save(product);
                })
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
}
