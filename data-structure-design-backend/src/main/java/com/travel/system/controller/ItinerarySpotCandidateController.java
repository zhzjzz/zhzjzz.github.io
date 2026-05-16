package com.travel.system.controller;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.service.ItinerarySpotCandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries/{itineraryId}/map-spots")
public class ItinerarySpotCandidateController {
    private final ItinerarySpotCandidateService candidateService;

    public ItinerarySpotCandidateController(ItinerarySpotCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public List<ItineraryMapSpot> list(@PathVariable Long itineraryId) {
        return candidateService.listMapSpots(itineraryId);
    }

    @PostMapping
    public ItinerarySpotCandidate add(@PathVariable Long itineraryId,
                                      @RequestBody ItinerarySpotCandidateRequest request) {
        try {
            return candidateService.addCandidate(itineraryId, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
