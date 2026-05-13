package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItinerarySpotVoteControllerTest {

    @Test
    void listsVotesForItinerary() {
        RecordingVoteService service = new RecordingVoteService();
        ItinerarySpotVote vote = new ItinerarySpotVote();
        vote.setItineraryId(7L);
        vote.setSpotId(101L);
        vote.setUsername("小周");
        vote.setVoteType("must");
        service.votes = List.of(vote);
        ItinerarySpotVoteController controller = new ItinerarySpotVoteController(service);

        List<ItinerarySpotVote> result = controller.list(7L);

        assertThat(result).containsExactly(vote);
        assertThat(service.listItineraryId).isEqualTo(7L);
    }

    @Test
    void savesVoteForItinerary() {
        RecordingVoteService service = new RecordingVoteService();
        ItinerarySpotVote saved = new ItinerarySpotVote();
        saved.setItineraryId(7L);
        saved.setSpotId(101L);
        saved.setUsername("小周");
        saved.setVoteType("must");
        service.savedVote = saved;
        ItinerarySpotVoteController controller = new ItinerarySpotVoteController(service);
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("must");

        ItinerarySpotVote result = controller.vote(7L, message);

        assertThat(result).isSameAs(saved);
        assertThat(service.savedItineraryId).isEqualTo(7L);
        assertThat(service.savedMessage).isSameAs(message);
    }

    @Test
    void mapsValidationFailureToBadRequest() {
        RecordingVoteService service = new RecordingVoteService();
        service.failure = new IllegalArgumentException("voteType must be one of must, want, avoid, backup");
        ItinerarySpotVoteController controller = new ItinerarySpotVoteController(service);
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();

        assertThatThrownBy(() -> controller.vote(7L, message))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static class RecordingVoteService extends ItinerarySpotVoteService {
        private Long listItineraryId;
        private List<ItinerarySpotVote> votes = List.of();
        private Long savedItineraryId;
        private ItinerarySpotVoteMessage savedMessage;
        private ItinerarySpotVote savedVote;
        private RuntimeException failure;

        RecordingVoteService() {
            super(new NoopVoteMapper());
        }

        @Override
        public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
            this.listItineraryId = itineraryId;
            return votes;
        }

        @Override
        public ItinerarySpotVote saveVote(Long itineraryId, ItinerarySpotVoteMessage message) {
            if (failure != null) {
                throw failure;
            }
            this.savedItineraryId = itineraryId;
            this.savedMessage = message;
            return savedVote;
        }
    }

    private static class NoopVoteMapper implements ItinerarySpotVoteMapper {
        @Override
        public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
            return List.of();
        }

        @Override
        public ItinerarySpotVote findByUnique(Long itineraryId, Long spotId, String username) {
            return null;
        }

        @Override
        public void insert(ItinerarySpotVote vote) {
        }

        @Override
        public int updateByUnique(ItinerarySpotVote vote) {
            return 0;
        }
    }
}
