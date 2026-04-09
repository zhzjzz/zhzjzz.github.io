package com.travel.system.controller;

import com.travel.system.model.Itinerary;
import com.travel.system.repository.ItineraryRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 协作行程控制器。
 * 对外提供行程创建与查询接口，支撑多人协作规划场景。
 */
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {
    private final ItineraryRepository itineraryRepository;

    public ItineraryController(ItineraryRepository itineraryRepository) {
        this.itineraryRepository = itineraryRepository;
    }

    /**
     * 查询全部行程。
     */
    @GetMapping
    public List<Itinerary> list() {
        return itineraryRepository.findAll();
    }

    /**
     * 创建行程，并在服务端写入更新时间，便于后续协作同步。
     */
    @PostMapping
    public Itinerary create(@RequestBody Itinerary itinerary) {
        itinerary.setUpdatedAt(LocalDateTime.now());
        return itineraryRepository.save(itinerary);
    }
}
