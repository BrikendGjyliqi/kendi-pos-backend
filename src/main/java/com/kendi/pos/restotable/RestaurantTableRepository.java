package com.kendi.pos.restotable;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findAllBySection(Section section);
    List<RestaurantTable> findAllByStatus(TableStatus status);
    boolean existsByName(String name);
}