package com.bistrodungu.menu.infrastructure.web;

import com.bistrodungu.menu.application.service.ProductService;
import com.bistrodungu.menu.infrastructure.persistence.ProductEntity;
import com.bistrodungu.menu.infrastructure.web.dto.ProductDTO;
import com.bistrodungu.shared.domain.vo.TenantId;
import com.bistrodungu.shared.infrastructure.api.ApiResponse;
import com.bistrodungu.shared.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/menu/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProducts() {
        TenantId tenantId = tenantContext.getTenant();
        List<ProductDTO> products = productService.getActiveProducts(tenantId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable UUID id) {
        TenantId tenantId = tenantContext.getTenant();
        ProductEntity product = productService.getProduct(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(toDTO(product)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String description
    ) {
        TenantId tenantId = tenantContext.getTenant();
        ProductEntity product = productService.createProduct(tenantId, name, price, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toDTO(product)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String description
    ) {
        TenantId tenantId = tenantContext.getTenant();
        ProductEntity product = productService.updateProduct(id, tenantId, name, price, description);
        return ResponseEntity.ok(ApiResponse.ok(toDTO(product)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        TenantId tenantId = tenantContext.getTenant();
        productService.deleteProduct(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private ProductDTO toDTO(ProductEntity entity) {
        return new ProductDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategoryId() != null ? entity.getCategoryId().toString() : null,
                entity.getPrice(),
                entity.isActive()
        );
    }
}
