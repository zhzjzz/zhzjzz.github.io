package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
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
@RequestMapping("/api/itineraries/{itineraryId}/spot-votes")
public class ItinerarySpotVoteController {
    private final ItinerarySpotVoteService voteService;

    public ItinerarySpotVoteController(ItinerarySpotVoteService voteService) {
        this.voteService = voteService;
    }

    @GetMapping
    public List<ItinerarySpotVote> list(@PathVariable Long itineraryId) {
        return voteService.findByItineraryId(itineraryId);
    }

    @PostMapping
    public ItinerarySpotVote vote(@PathVariable Long itineraryId,
                                  @RequestBody ItinerarySpotVoteMessage message) {
        try {
            return voteService.saveVote(itineraryId, message);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
