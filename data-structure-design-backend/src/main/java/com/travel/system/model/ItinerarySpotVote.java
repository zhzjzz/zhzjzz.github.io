package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItinerarySpotVote {
    private Long id;
    private Long itineraryId;
    private Long spotId;
    private String spotName;
    private String username;
    private String voteType;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
