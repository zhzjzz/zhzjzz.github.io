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
    /**
     * 说明该方法的业务职责、主要输入和返回结果，便于维护时快速理解调用边界。
     */

    @MessageMapping("/itinerary/{id}/edit")
    public void editItinerary(@DestinationVariable Long id, ItineraryEditMessage message) {
        message.setItineraryId(id);
        itineraryService.handleEdit(message);
    }
}
