package com.travel.system.controller;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.service.ItineraryPlannerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-planner")
public class ItineraryPlannerController {
    private final ItineraryPlannerService plannerService;

    public ItineraryPlannerController(ItineraryPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping("/preview")
    public ItineraryPlannerPreviewResponse preview(@RequestBody ItineraryPlannerPreviewRequest request) {
        return plannerService.preview(request);
    }
}
