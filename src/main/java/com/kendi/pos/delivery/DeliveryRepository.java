package com.kendi.pos.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    List<Delivery> findBySupplierIdOrderByDeliveryDateDesc(String supplierId);
    List<Delivery> findAllByOrderByDeliveryDateDesc();
}