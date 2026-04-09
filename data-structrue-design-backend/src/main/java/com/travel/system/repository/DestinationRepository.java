package com.travel.system.repository;

import com.travel.system.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);
}
