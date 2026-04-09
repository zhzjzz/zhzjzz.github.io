package com.travel.system.controller;

import com.travel.system.model.Destination;
import com.travel.system.repository.DestinationRepository;
import com.travel.system.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationRepository destinationRepository;
    private final RecommendationService recommendationService;

    public DestinationController(DestinationRepository destinationRepository, RecommendationService recommendationService) {
        this.destinationRepository = destinationRepository;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<Destination> list(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return destinationRepository.findAll();
        }
        return destinationRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
    }

    @GetMapping("/top")
    public List<Destination> top(@RequestParam(defaultValue = "10") int k) {
        return recommendationService.topKDestinations(destinationRepository.findAll(), k);
    }

    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        return destinationRepository.save(destination);
    }
}
