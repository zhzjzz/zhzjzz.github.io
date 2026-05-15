package com.travel.system.controller;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotCandidateService;
import com.travel.system.service.ItinerarySpotVoteService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryVoteWebSocketControllerTest {

    @Test
    void broadcastsUpdatedVoteAfterSave() {
        RecordingVoteService service = new RecordingVoteService();
        RecordingCandidateService candidateService = new RecordingCandidateService();
        RecordingMessagingTemplate messagingTemplate = new RecordingMessagingTemplate();
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(
                service, candidateService, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("West Lake");
        message.setUsername("Zhou");
        message.setVoteType("must");
        message.setReason("night view");

        ItinerarySpotVote saved = vote();
        service.saved = saved;
        service.votes = List.of(saved);
        ItineraryMapSpot mapSpot = mapSpot(saved);
        candidateService.mapSpots = List.of(mapSpot);

        controller.vote(7L, message);

        ItinerarySpotVoteBroadcastMessage broadcast =
                (ItinerarySpotVoteBroadcastMessage) messagingTemplate.payload;
        assertThat(messagingTemplate.destination).isEqualTo("/topic/itinerary/7");
        assertThat(broadcast.getType()).isEqualTo(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED);
        assertThat(broadcast.getVote()).isSameAs(saved);
        assertThat(broadcast.getVotes()).containsExactly(saved);
        assertThat(broadcast.getMapSpots()).containsExactly(mapSpot);
        assertThat(broadcast.getUsername()).isEqualTo("Zhou");
    }

    @Test
    void broadcastsRejectedVoteWhenValidationFails() {
        RecordingVoteService service = new RecordingVoteService();
        service.failure = new IllegalArgumentException("voteType must be one of must, want, avoid, backup");
        RecordingCandidateService candidateService = new RecordingCandidateService();
        RecordingMessagingTemplate messagingTemplate = new RecordingMessagingTemplate();
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(
                service, candidateService, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setUsername("Zhou");

        controller.vote(7L, message);

        ItinerarySpotVoteBroadcastMessage broadcast =
                (ItinerarySpotVoteBroadcastMessage) messagingTemplate.payload;
        assertThat(messagingTemplate.destination).isEqualTo("/topic/itinerary/7");
        assertThat(broadcast.getType()).isEqualTo(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED);
        assertThat(broadcast.getMessage()).contains("voteType");
        assertThat(broadcast.getMapSpots()).isEmpty();
        assertThat(broadcast.getUsername()).isEqualTo("Zhou");
    }

    private static ItinerarySpotVote vote() {
        ItinerarySpotVote saved = new ItinerarySpotVote();
        saved.setId(1L);
        saved.setItineraryId(7L);
        saved.setSpotId(101L);
        saved.setSpotName("West Lake");
        saved.setUsername("Zhou");
        saved.setVoteType("must");
        saved.setReason("night view");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        return saved;
    }

    private static ItineraryMapSpot mapSpot(ItinerarySpotVote saved) {
        ItineraryMapSpot mapSpot = new ItineraryMapSpot();
        mapSpot.setDestinationId(101L);
        mapSpot.setSpotId(101L);
        mapSpot.setSpotName("West Lake");
        mapSpot.setLatitude(30.259244);
        mapSpot.setLongitude(120.13026);
        mapSpot.setVotes(List.of(saved));
        return mapSpot;
    }

    private static class RecordingVoteService extends ItinerarySpotVoteService {
        private ItinerarySpotVote saved;
        private List<ItinerarySpotVote> votes = List.of();
        private IllegalArgumentException failure;

        RecordingVoteService() {
            super(null, null);
        }

        @Override
        public ItinerarySpotVote saveVote(Long itineraryId, ItinerarySpotVoteMessage message) {
            if (failure != null) {
                throw failure;
            }
            return saved;
        }

        @Override
        public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
            return votes;
        }
    }

    private static class RecordingCandidateService extends ItinerarySpotCandidateService {
        private List<ItineraryMapSpot> mapSpots = List.of();

        RecordingCandidateService() {
            super(null, null, null);
        }

        @Override
        public List<ItineraryMapSpot> listMapSpots(Long itineraryId) {
            return mapSpots;
        }
    }

    private static class RecordingMessagingTemplate extends SimpMessagingTemplate {
        private String destination;
        private Object payload;

        RecordingMessagingTemplate() {
            super(new NoopMessageChannel());
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            this.destination = destination;
            this.payload = payload;
        }
    }

    private static class NoopMessageChannel implements MessageChannel {
        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }
}
