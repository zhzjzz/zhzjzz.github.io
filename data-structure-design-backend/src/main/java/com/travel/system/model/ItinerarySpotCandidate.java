package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItinerarySpotCandidate {
    private Long id;
    private Long itineraryId;
    private Long destinationId;
    private String spotName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
