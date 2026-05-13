package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ItineraryVoteWebSocketController {
    private final ItinerarySpotVoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;

    public ItineraryVoteWebSocketController(ItinerarySpotVoteService voteService,
                                            SimpMessagingTemplate messagingTemplate) {
        this.voteService = voteService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/itinerary/{id}/spot-vote")
    public void vote(@DestinationVariable Long id, ItinerarySpotVoteMessage message) {
        try {
            message.setItineraryId(id);
            ItinerarySpotVote saved = voteService.saveVote(id, message);
            List<ItinerarySpotVote> votes = voteService.findByItineraryId(id);
            messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                    new ItinerarySpotVoteBroadcastMessage(
                            ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED,
                            saved.getUsername(),
                            saved,
                            votes,
                            null,
                            LocalDateTime.now()));
        } catch (IllegalArgumentException ex) {
            String username = message == null ? "" : message.getUsername();
            messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                    new ItinerarySpotVoteBroadcastMessage(
                            ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED,
                            username,
                            null,
                            List.of(),
                            ex.getMessage(),
                            LocalDateTime.now()));
        }
    }
}
