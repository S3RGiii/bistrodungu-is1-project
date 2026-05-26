package com.bistrodungu.menu.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        String description,
        UUID categoryId,
        BigDecimal price,
        boolean isActive
) {}
