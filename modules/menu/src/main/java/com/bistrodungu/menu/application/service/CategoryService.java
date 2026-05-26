package com.bistrodungu.menu.application.service;

import com.bistrodungu.menu.infrastructure.persistence.CategoryEntity;
import com.bistrodungu.menu.infrastructure.persistence.CategoryJpaRepository;
import com.bistrodungu.shared.domain.vo.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing menu categories
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryJpaRepository categoryRepository;

    public CategoryEntity createCategory(TenantId tenantId, String name, String description) {
        CategoryEntity category = new CategoryEntity(tenantId, name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    public CategoryEntity getCategory(UUID categoryId, TenantId tenantId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getTenantId().equals(tenantId.value()))
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    public List<CategoryEntity> getActiveCategories(TenantId tenantId) {
        return categoryRepository.findByTenantIdAndIsActiveTrue(tenantId.value());
    }

    public CategoryEntity updateCategory(UUID categoryId, TenantId tenantId, String name, String description) {
        CategoryEntity category = getCategory(categoryId, tenantId);
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    public void deleteCategory(UUID categoryId, TenantId tenantId) {
        CategoryEntity category = getCategory(categoryId, tenantId);
        category.setDeleted(true);
        categoryRepository.save(category);
    }
}
