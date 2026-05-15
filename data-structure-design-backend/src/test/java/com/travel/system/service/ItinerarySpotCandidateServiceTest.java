package com.travel.system.service;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.mapper.ItinerarySpotCandidateMapper;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.model.ItinerarySpotVote;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItinerarySpotCandidateServiceTest {
    private final FakeCandidateMapper candidateMapper = new FakeCandidateMapper();
    private final FakeVoteMapper voteMapper = new FakeVoteMapper();
    private final FakeDestinationService destinationService = new FakeDestinationService();
    private final ItinerarySpotCandidateService service =
            new ItinerarySpotCandidateService(candidateMapper, voteMapper, destinationService);

    @Test
    void addsRealDestinationWithCoordinatesAsCandidate() {
        destinationService.destinations.add(new Destination(
                11L, "West Lake", "scenic", "lake", 98.0, 4.9,
                "real destination", 30.259244, 120.13026));

        ItinerarySpotCandidateRequest request = new ItinerarySpotCandidateRequest();
        request.setDestinationId(11L);
        ItinerarySpotCandidate candidate = service.addCandidate(7L, request);

        assertThat(candidate.getItineraryId()).isEqualTo(7L);
        assertThat(candidate.getDestinationId()).isEqualTo(11L);
        assertThat(candidate.getSpotName()).isEqualTo("West Lake");
        assertThat(candidate.getLatitude()).isEqualTo(30.259244);
        assertThat(candidate.getLongitude()).isEqualTo(120.13026);
    }

    @Test
    void rejectsDestinationWithoutCoordinates() {
        destinationService.destinations.add(new Destination(
                12L, "Missing Coordinates", "scenic", "unknown", 1.0, 1.0,
                "missing coords", null, null));
        ItinerarySpotCandidateRequest request = new ItinerarySpotCandidateRequest();
        request.setDestinationId(12L);

        assertThatThrownBy(() -> service.addCandidate(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coordinates");
    }

    @Test
    void buildsMapSpotsWithVotesGroupedByDestination() {
        candidateMapper.rows.add(candidate(7L, 11L, "West Lake", 30.259244, 120.13026));
        voteMapper.rows.add(vote(7L, 11L, "West Lake", "Zhou", "must"));
        voteMapper.rows.add(vote(7L, 11L, "West Lake", "Lin", "avoid"));

        List<ItineraryMapSpot> spots = service.listMapSpots(7L);

        assertThat(spots).hasSize(1);
        assertThat(spots.get(0).getDestinationId()).isEqualTo(11L);
        assertThat(spots.get(0).getSpotId()).isEqualTo(11L);
        assertThat(spots.get(0).getSpotName()).isEqualTo("West Lake");
        assertThat(spots.get(0).getLatitude()).isEqualTo(30.259244);
        assertThat(spots.get(0).getLongitude()).isEqualTo(120.13026);
        assertThat(spots.get(0).getVotes()).hasSize(2);
    }

    private static ItinerarySpotCandidate candidate(Long itineraryId, Long destinationId, String name,
                                                    Double latitude, Double longitude) {
        ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
        candidate.setId(destinationId);
        candidate.setItineraryId(itineraryId);
        candidate.setDestinationId(destinationId);
        candidate.setSpotName(name);
        candidate.setLatitude(latitude);
        candidate.setLongitude(longitude);
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());
        return candidate;
    }

    private static ItinerarySpotVote vote(Long itineraryId, Long spotId, String spotName,
                                          String username, String voteType) {
        ItinerarySpotVote vote = new ItinerarySpotVote();
        vote.setId((long) username.hashCode());
        vote.setItineraryId(itineraryId);
        vote.setSpotId(spotId);
        vote.setSpotName(spotName);
        vote.setUsername(username);
        vote.setVoteType(voteType);
        vote.setReason("");
        vote.setCreatedAt(LocalDateTime.now());
        vote.setUpdatedAt(LocalDateTime.now());
        return vote;
    }

    private static class FakeDestinationService extends DestinationService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationService() {
            super(null, null);
        }

        @Override
        public Destination findById(Long id) {
            return destinations.stream()
                    .filter(destination -> Objects.equals(destination.getId(), id))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static class FakeCandidateMapper implements ItinerarySpotCandidateMapper {
        private final List<ItinerarySpotCandidate> rows = new ArrayList<>();

        @Override
        public List<ItinerarySpotCandidate> findByItineraryId(Long itineraryId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .toList();
        }

        @Override
        public ItinerarySpotCandidate findByUnique(Long itineraryId, Long destinationId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .filter(row -> Objects.equals(row.getDestinationId(), destinationId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void insert(ItinerarySpotCandidate candidate) {
            candidate.setId((long) rows.size() + 1);
            rows.add(candidate);
        }

        @Override
        public int updateByUnique(ItinerarySpotCandidate candidate) {
            ItinerarySpotCandidate existing = findByUnique(candidate.getItineraryId(), candidate.getDestinationId());
            if (existing == null) {
                return 0;
            }
            existing.setSpotName(candidate.getSpotName());
            existing.setLatitude(candidate.getLatitude());
            existing.setLongitude(candidate.getLongitude());
            existing.setUpdatedAt(candidate.getUpdatedAt());
            return 1;
        }
    }

    private static class FakeVoteMapper implements ItinerarySpotVoteMapper {
        private final List<ItinerarySpotVote> rows = new ArrayList<>();

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
            rows.add(vote);
        }

        @Override
        public int updateByUnique(ItinerarySpotVote vote) {
            return 0;
        }
    }
}
