package com.bistrodungu.menu.infrastructure.web;

import com.bistrodungu.menu.application.service.CategoryService;
import com.bistrodungu.menu.infrastructure.persistence.CategoryEntity;
import com.bistrodungu.menu.infrastructure.web.dto.CategoryDTO;
import com.bistrodungu.shared.domain.vo.TenantId;
import com.bistrodungu.shared.infrastructure.api.ApiResponse;
import com.bistrodungu.shared.infrastructure.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/menu/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        TenantId tenantId = tenantContext.getTenant();
        List<CategoryDTO> categories = categoryService.getActiveCategories(tenantId)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategory(@PathVariable UUID id) {
        TenantId tenantId = tenantContext.getTenant();
        CategoryEntity category = categoryService.getCategory(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(toDTO(category)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {
        TenantId tenantId = tenantContext.getTenant();
        CategoryEntity category = categoryService.createCategory(tenantId, name, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toDTO(category)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {
        TenantId tenantId = tenantContext.getTenant();
        CategoryEntity category = categoryService.updateCategory(id, tenantId, name, description);
        return ResponseEntity.ok(ApiResponse.ok(toDTO(category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        TenantId tenantId = tenantContext.getTenant();
        categoryService.deleteCategory(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private CategoryDTO toDTO(CategoryEntity entity) {
        return new CategoryDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.isActive()
        );
    }
}
