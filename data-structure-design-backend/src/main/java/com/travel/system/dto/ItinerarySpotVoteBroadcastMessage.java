package com.travel.system.dto;

import com.travel.system.model.ItinerarySpotVote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarySpotVoteBroadcastMessage {
    public enum Type { SPOT_VOTE_UPDATED, SPOT_VOTE_REJECTED }

    private Type type;
    private String username;
    private ItinerarySpotVote vote;
    private List<ItinerarySpotVote> votes;
    private String message;
    private LocalDateTime serverTimestamp;
}
