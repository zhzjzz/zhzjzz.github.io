package com.travel.system.repository;

import com.travel.system.model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findByFacilityTypeContainingIgnoreCase(String type);
}
