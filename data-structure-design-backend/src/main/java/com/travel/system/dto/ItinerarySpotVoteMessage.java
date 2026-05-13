package com.travel.system.dto;

import lombok.Data;

@Data
public class ItinerarySpotVoteMessage {
    private Long itineraryId;
    private Long spotId;
    private String spotName;
    private String username;
    private String voteType;
    private String reason;
}
