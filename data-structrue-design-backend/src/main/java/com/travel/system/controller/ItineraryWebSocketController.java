package com.travel.system.controller;

import com.travel.system.dto.ItineraryEditMessage;
import com.travel.system.service.ItineraryService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ItineraryWebSocketController {

    private final ItineraryService itineraryService;

    public ItineraryWebSocketController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @MessageMapping("/itinerary/{id}/edit")
    public void editItinerary(@DestinationVariable Long id, ItineraryEditMessage message) {
        message.setItineraryId(id);
        itineraryService.handleEdit(message);
    }
}
