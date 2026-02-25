package com.bistrodungu.service;

import com.bistrodungu.model.Order;
import com.bistrodungu.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public Order create(Order order) {
        return orderRepository.save(order);
    }

    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order existing = findById(id);
        existing.setStatus(status);
        return orderRepository.save(existing);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
