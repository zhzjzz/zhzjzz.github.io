package com.travel.system.controller;

import com.travel.system.model.Itinerary;
import com.travel.system.repository.ItineraryRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {
    private final ItineraryRepository itineraryRepository;

    public ItineraryController(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    @GetMapping
    public List<Itinerary> list() {
        return itineraryRepository.findAll();
    }

    @PostMapping
    public Itinerary create(@RequestBody Itinerary itinerary) {
        itinerary.setUpdatedAt(LocalDateTime.now());
        return itineraryRepository.save(itinerary);
    }
}
