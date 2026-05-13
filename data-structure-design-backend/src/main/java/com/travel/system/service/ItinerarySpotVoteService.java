package com.travel.system.service;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.ItinerarySpotVote;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ItinerarySpotVoteService {
    private static final Set<String> ALLOWED_TYPES = Set.of("must", "want", "avoid", "backup");

    private final ItinerarySpotVoteMapper voteMapper;

    public ItinerarySpotVoteService(ItinerarySpotVoteMapper voteMapper) {
        this.voteMapper = voteMapper;
    }

    public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        return voteMapper.findByItineraryId(itineraryId);
    }

    public ItinerarySpotVote saveVote(Long itineraryId, ItinerarySpotVoteMessage message) {
        validate(itineraryId, message);
        String username = message.getUsername().trim();
        String voteType = message.getVoteType().trim();
        LocalDateTime now = LocalDateTime.now();

        ItinerarySpotVote existing = voteMapper.findByUnique(itineraryId, message.getSpotId(), username);
        ItinerarySpotVote vote = new ItinerarySpotVote();
        vote.setItineraryId(itineraryId);
        vote.setSpotId(message.getSpotId());
        vote.setSpotName(message.getSpotName().trim());
        vote.setUsername(username);
        vote.setVoteType(voteType);
        vote.setReason(normalizeReason(message.getReason()));
        vote.setUpdatedAt(now);

        if (existing == null) {
            vote.setCreatedAt(now);
            voteMapper.insert(vote);
        } else {
            vote.setCreatedAt(existing.getCreatedAt());
            voteMapper.updateByUnique(vote);
        }

        return voteMapper.findByUnique(itineraryId, message.getSpotId(), username);
    }

    private void validate(Long itineraryId, ItinerarySpotVoteMessage message) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is required");
        }
        if (message.getSpotId() == null) {
            throw new IllegalArgumentException("spotId is required");
        }
        if (message.getSpotName() == null || message.getSpotName().isBlank()) {
            throw new IllegalArgumentException("spotName is required");
        }
        if (message.getUsername() == null || message.getUsername().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (message.getVoteType() == null || !ALLOWED_TYPES.contains(message.getVoteType().trim())) {
            throw new IllegalArgumentException("voteType must be one of must, want, avoid, backup");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.trim();
    }
}
