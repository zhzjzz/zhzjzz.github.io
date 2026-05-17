package com.travel.system.dto;

import com.travel.system.model.ItinerarySpotVote;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItineraryMapSpot {
    private Long candidateId;
    private Long destinationId;
    private Long spotId;
    private String spotName;
    private Double latitude;
    private Double longitude;
    private List<ItinerarySpotVote> votes = new ArrayList<>();
}
