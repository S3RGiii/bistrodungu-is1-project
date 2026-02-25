package com.bistrodungu.service;

import com.bistrodungu.model.RestaurantTable;
import com.bistrodungu.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantTableService {

    private final RestaurantTableRepository tableRepository;

    public List<RestaurantTable> findAll() {
        return tableRepository.findAll();
    }

    public List<RestaurantTable> findAvailable() {
        return tableRepository.findByStatus(RestaurantTable.TableStatus.AVAILABLE);
    }

    public RestaurantTable findById(Long id) {
        return tableRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Table not found with id: " + id));
    }

    public RestaurantTable create(RestaurantTable table) {
        return tableRepository.save(table);
    }

    public RestaurantTable update(Long id, RestaurantTable updated) {
        RestaurantTable existing = findById(id);
        existing.setTableNumber(updated.getTableNumber());
        existing.setCapacity(updated.getCapacity());
        existing.setStatus(updated.getStatus());
        return tableRepository.save(existing);
    }

    public void delete(Long id) {
        tableRepository.deleteById(id);
    }
}
