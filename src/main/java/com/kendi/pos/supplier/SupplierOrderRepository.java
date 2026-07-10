package com.kendi.pos.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, String> {
    List<SupplierOrder> findBySupplierIdOrderByCreatedAtDesc(String supplierId);
    List<SupplierOrder> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}