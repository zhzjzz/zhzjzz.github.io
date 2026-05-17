package com.travel.system.service;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.mapper.ItinerarySpotCandidateMapper;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.model.ItinerarySpotVote;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItinerarySpotCandidateService {
    private final ItinerarySpotCandidateMapper candidateMapper;
    private final ItinerarySpotVoteMapper voteMapper;
    private final DestinationService destinationService;

    public ItinerarySpotCandidateService(ItinerarySpotCandidateMapper candidateMapper,
                                         ItinerarySpotVoteMapper voteMapper,
                                         DestinationService destinationService) {
        this.candidateMapper = candidateMapper;
        this.voteMapper = voteMapper;
        this.destinationService = destinationService;
    }

    public List<ItinerarySpotCandidate> findByItineraryId(Long itineraryId) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        return candidateMapper.findByItineraryId(itineraryId);
    }

    public boolean exists(Long itineraryId, Long destinationId) {
        if (itineraryId == null || destinationId == null) {
            return false;
        }
        return candidateMapper.findByUnique(itineraryId, destinationId) != null;
    }

    public ItinerarySpotCandidate addCandidate(Long itineraryId, ItinerarySpotCandidateRequest request) {
        validateRequest(itineraryId, request);
        Destination destination = destinationService.findById(request.getDestinationId());
        if (destination == null) {
            throw new IllegalArgumentException("destination does not exist");
        }
        if (destination.getLatitude() == null || destination.getLongitude() == null) {
            throw new IllegalArgumentException("destination coordinates are required");
        }

        LocalDateTime now = LocalDateTime.now();
        ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
        candidate.setItineraryId(itineraryId);
        candidate.setDestinationId(destination.getId());
        candidate.setSpotName(destination.getName());
        candidate.setLatitude(destination.getLatitude());
        candidate.setLongitude(destination.getLongitude());
        candidate.setUpdatedAt(now);

        ItinerarySpotCandidate existing = candidateMapper.findByUnique(itineraryId, destination.getId());
        if (existing == null) {
            candidate.setCreatedAt(now);
            candidateMapper.insert(candidate);
        } else {
            candidate.setCreatedAt(existing.getCreatedAt());
            candidateMapper.updateByUnique(candidate);
        }
        return candidateMapper.findByUnique(itineraryId, destination.getId());
    }

    public List<ItineraryMapSpot> listMapSpots(Long itineraryId) {
        List<ItinerarySpotCandidate> candidates = findByItineraryId(itineraryId);
        Map<Long, List<ItinerarySpotVote>> votesBySpotId = voteMapper.findByItineraryId(itineraryId).stream()
                .collect(Collectors.groupingBy(ItinerarySpotVote::getSpotId));

        return candidates.stream().map(candidate -> {
            ItineraryMapSpot spot = new ItineraryMapSpot();
            spot.setCandidateId(candidate.getId());
            spot.setDestinationId(candidate.getDestinationId());
            spot.setSpotId(candidate.getDestinationId());
            spot.setSpotName(candidate.getSpotName());
            spot.setLatitude(candidate.getLatitude());
            spot.setLongitude(candidate.getLongitude());
            spot.setVotes(votesBySpotId.getOrDefault(candidate.getDestinationId(), List.of()));
            return spot;
        }).toList();
    }

    private void validateRequest(Long itineraryId, ItinerarySpotCandidateRequest request) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        if (request == null || request.getDestinationId() == null) {
            throw new IllegalArgumentException("destinationId is required");
        }
    }
}
