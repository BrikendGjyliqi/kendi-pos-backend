package com.kendi.pos.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByTableId(String tableId);
    List<Order> findByStatus(String status);
    List<Order> findByTableIdAndStatus(String tableId, String status);
    List<Order> findByStaffIdAndStatus(String staffId, String status);
}