package com.travel.system.dto;

import com.travel.system.model.Itinerary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryImportCreateResponse {
    private Itinerary itinerary;
    private ItineraryImportResponse importResult;
    private List<ItineraryMapSpot> plannerSpots = new ArrayList<>();
}
