package com.travel.system.repository;

import com.travel.system.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByNameContainingIgnoreCaseOrCuisineContainingIgnoreCaseOrStoreNameContainingIgnoreCase(
            String name, String cuisine, String storeName);
}
