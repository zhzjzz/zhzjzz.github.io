package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotCandidateService;
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
    private final ItinerarySpotCandidateService candidateService;
    private final SimpMessagingTemplate messagingTemplate;

    public ItineraryVoteWebSocketController(ItinerarySpotVoteService voteService,
                                            ItinerarySpotCandidateService candidateService,
                                            SimpMessagingTemplate messagingTemplate) {
        this.voteService = voteService;
        this.candidateService = candidateService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/itinerary/{id}/spot-vote")
    public void vote(@DestinationVariable Long id, ItinerarySpotVoteMessage message) {
        try {
            message.setItineraryId(id);
            ItinerarySpotVote saved = voteService.saveVote(id, message);
            List<ItinerarySpotVote> votes = voteService.findByItineraryId(id);
            ItinerarySpotVoteBroadcastMessage broadcast = new ItinerarySpotVoteBroadcastMessage();
            broadcast.setType(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED);
            broadcast.setUsername(saved.getUsername());
            broadcast.setVote(saved);
            broadcast.setVotes(votes);
            broadcast.setMapSpots(candidateService.listMapSpots(id));
            broadcast.setServerTimestamp(LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/itinerary/" + id, broadcast);
        } catch (IllegalArgumentException ex) {
            String username = message == null ? "" : message.getUsername();
            ItinerarySpotVoteBroadcastMessage broadcast = new ItinerarySpotVoteBroadcastMessage();
            broadcast.setType(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED);
            broadcast.setUsername(username);
            broadcast.setVote(null);
            broadcast.setVotes(List.of());
            broadcast.setMapSpots(List.of());
            broadcast.setMessage(ex.getMessage());
            broadcast.setServerTimestamp(LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/itinerary/" + id, broadcast);
        }
    }
}
