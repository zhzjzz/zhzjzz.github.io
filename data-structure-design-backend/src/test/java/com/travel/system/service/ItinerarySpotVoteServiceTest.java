package com.travel.system.service;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.ItinerarySpotVote;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItinerarySpotVoteServiceTest {

    private final FakeVoteMapper mapper = new FakeVoteMapper();
    private final ItinerarySpotVoteService service = new ItinerarySpotVoteService(mapper);

    @Test
    void createsVoteWhenUserHasNotVotedForSpot() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("must");
        message.setReason("夜景必看");

        ItinerarySpotVote vote = service.saveVote(7L, message);

        assertThat(vote.getId()).isEqualTo(1L);
        assertThat(vote.getItineraryId()).isEqualTo(7L);
        assertThat(vote.getSpotId()).isEqualTo(101L);
        assertThat(vote.getSpotName()).isEqualTo("外滩");
        assertThat(vote.getUsername()).isEqualTo("小周");
        assertThat(vote.getVoteType()).isEqualTo("must");
        assertThat(vote.getReason()).isEqualTo("夜景必看");
        assertThat(vote.getCreatedAt()).isNotNull();
        assertThat(vote.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatesExistingVoteForSameItinerarySpotAndUser() {
        ItinerarySpotVoteMessage first = new ItinerarySpotVoteMessage();
        first.setSpotId(101L);
        first.setSpotName("外滩");
        first.setUsername("小周");
        first.setVoteType("want");
        first.setReason("顺路");
        service.saveVote(7L, first);

        ItinerarySpotVoteMessage second = new ItinerarySpotVoteMessage();
        second.setSpotId(101L);
        second.setSpotName("外滩");
        second.setUsername("小周");
        second.setVoteType("avoid");
        second.setReason("时间太紧");
        ItinerarySpotVote updated = service.saveVote(7L, second);

        assertThat(mapper.rows).hasSize(1);
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getVoteType()).isEqualTo("avoid");
        assertThat(updated.getReason()).isEqualTo("时间太紧");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    void rejectsInvalidVoteType() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("favorite");

        assertThatThrownBy(() -> service.saveVote(7L, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voteType");
    }

    @Test
    void rejectsMissingUsername() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername(" ");
        message.setVoteType("must");

        assertThatThrownBy(() -> service.saveVote(7L, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    private static class FakeVoteMapper implements ItinerarySpotVoteMapper {
        private final List<ItinerarySpotVote> rows = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .toList();
        }

        @Override
        public ItinerarySpotVote findByUnique(Long itineraryId, Long spotId, String username) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .filter(row -> Objects.equals(row.getSpotId(), spotId))
                    .filter(row -> Objects.equals(row.getUsername(), username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void insert(ItinerarySpotVote vote) {
            vote.setId(nextId++);
            rows.add(copy(vote));
        }

        @Override
        public int updateByUnique(ItinerarySpotVote vote) {
            ItinerarySpotVote existing = findByUnique(vote.getItineraryId(), vote.getSpotId(), vote.getUsername());
            if (existing == null) {
                return 0;
            }
            existing.setSpotName(vote.getSpotName());
            existing.setVoteType(vote.getVoteType());
            existing.setReason(vote.getReason());
            existing.setUpdatedAt(vote.getUpdatedAt());
            return 1;
        }

        private ItinerarySpotVote copy(ItinerarySpotVote vote) {
            ItinerarySpotVote copy = new ItinerarySpotVote();
            copy.setId(vote.getId());
            copy.setItineraryId(vote.getItineraryId());
            copy.setSpotId(vote.getSpotId());
            copy.setSpotName(vote.getSpotName());
            copy.setUsername(vote.getUsername());
            copy.setVoteType(vote.getVoteType());
            copy.setReason(vote.getReason());
            copy.setCreatedAt(vote.getCreatedAt());
            copy.setUpdatedAt(vote.getUpdatedAt());
            return copy;
        }
    }
}
