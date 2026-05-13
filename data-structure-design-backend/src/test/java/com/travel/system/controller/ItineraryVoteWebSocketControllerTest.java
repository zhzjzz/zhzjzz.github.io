package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItineraryVoteWebSocketControllerTest {

    @Test
    void broadcastsUpdatedVoteAfterSave() {
        ItinerarySpotVoteService service = mock(ItinerarySpotVoteService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(service, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("must");
        message.setReason("夜景必看");

        ItinerarySpotVote saved = new ItinerarySpotVote();
        saved.setId(1L);
        saved.setItineraryId(7L);
        saved.setSpotId(101L);
        saved.setSpotName("外滩");
        saved.setUsername("小周");
        saved.setVoteType("must");
        saved.setReason("夜景必看");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(service.saveVote(7L, message)).thenReturn(saved);
        when(service.findByItineraryId(7L)).thenReturn(List.of(saved));

        controller.vote(7L, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    ItinerarySpotVoteBroadcastMessage broadcast = (ItinerarySpotVoteBroadcastMessage) payload;
                    return broadcast.getType() == ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED
                            && broadcast.getVote() == saved
                            && broadcast.getVotes().size() == 1
                            && "小周".equals(broadcast.getUsername());
                }));
    }

    @Test
    void broadcastsRejectedVoteWhenValidationFails() {
        ItinerarySpotVoteService service = mock(ItinerarySpotVoteService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(service, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setUsername("小周");
        when(service.saveVote(7L, message)).thenThrow(new IllegalArgumentException("voteType must be one of must, want, avoid, backup"));

        controller.vote(7L, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"),
                org.mockito.ArgumentMatchers.<Object>argThat(payload -> {
                    ItinerarySpotVoteBroadcastMessage broadcast = (ItinerarySpotVoteBroadcastMessage) payload;
                    return broadcast.getType() == ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED
                            && broadcast.getMessage().contains("voteType")
                            && "小周".equals(broadcast.getUsername());
                }));
    }
}
