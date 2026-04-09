package com.travel.system.controller;

import com.travel.system.model.Facility;
import com.travel.system.repository.FacilityRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {
    private final FacilityRepository facilityRepository;

    public FacilityController(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @GetMapping
    public List<Facility> list(@RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) {
            return facilityRepository.findAll();
        }
        return facilityRepository.findByFacilityTypeContainingIgnoreCase(type);
    }
}
