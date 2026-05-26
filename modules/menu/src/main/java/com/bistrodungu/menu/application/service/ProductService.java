package com.bistrodungu.menu.application.service;

import com.bistrodungu.menu.infrastructure.persistence.ProductEntity;
import com.bistrodungu.menu.infrastructure.persistence.ProductJpaRepository;
import com.bistrodungu.shared.domain.vo.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing menu products
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductJpaRepository productRepository;

    public ProductEntity createProduct(TenantId tenantId, String name, BigDecimal price, String description) {
        ProductEntity product = new ProductEntity(tenantId, name, price);
        product.setDescription(description);
        return productRepository.save(product);
    }

    public ProductEntity getProduct(UUID productId, TenantId tenantId) {
        return productRepository.findById(productId)
                .filter(p -> p.getTenantId().equals(tenantId.value()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public List<ProductEntity> getActiveProducts(TenantId tenantId) {
        return productRepository.findByTenantIdAndIsActiveTrue(tenantId.value());
    }

    public ProductEntity updateProduct(UUID productId, TenantId tenantId, String name,
                                      BigDecimal price, String description) {
        ProductEntity product = getProduct(productId, tenantId);
        product.setName(name);
        product.setPrice(price);
        product.setDescription(description);
        return productRepository.save(product);
    }

    public void deleteProduct(UUID productId, TenantId tenantId) {
        ProductEntity product = getProduct(productId, tenantId);
        product.setDeleted(true);
        productRepository.save(product);
    }
}
