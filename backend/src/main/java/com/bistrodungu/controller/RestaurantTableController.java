package com.bistrodungu.controller;

import com.bistrodungu.model.RestaurantTable;
import com.bistrodungu.service.RestaurantTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    @GetMapping
    public ResponseEntity<List<RestaurantTable>> getAll() {
        return ResponseEntity.ok(tableService.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<RestaurantTable>> getAvailable() {
        return ResponseEntity.ok(tableService.findAvailable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantTable> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tableService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RestaurantTable> create(@RequestBody RestaurantTable table) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(table));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTable> update(@PathVariable Long id, @RequestBody RestaurantTable table) {
        return ResponseEntity.ok(tableService.update(id, table));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
