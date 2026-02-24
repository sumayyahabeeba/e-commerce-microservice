package com.ecommerce.product.repository;

import com.ecommerce.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.active = true")
    List<Product> findAllInStock();

    boolean existsByNameIgnoreCase(String name);
}
