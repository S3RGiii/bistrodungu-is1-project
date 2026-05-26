package com.bistrodungu.menu.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryDTO(
        UUID id,
        String name,
        String description,
        Integer sortOrder,
        boolean isActive
) {}
