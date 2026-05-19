package com.travel.system.controller;

import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.service.ItineraryImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-import")
public class ItineraryImportController {
    private final ItineraryImportService importService;

    public ItineraryImportController(ItineraryImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    public ItineraryImportResponse preview(@RequestBody ItineraryImportRequest request) {
        return importService.preview(request);
    }

    @PostMapping("/create")
    public ItineraryImportCreateResponse create(@RequestBody ItineraryImportRequest request) {
        return importService.create(request);
    }
}
