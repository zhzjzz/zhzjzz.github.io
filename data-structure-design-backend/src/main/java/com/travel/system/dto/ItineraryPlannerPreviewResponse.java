package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlannerPreviewResponse {
    private List<OrderedSpot> orderedSpots = new ArrayList<>();
    private List<MultiSpotNavigationResponse.RouteSegment> segments = new ArrayList<>();
    private Double totalDistance = 0.0;
    private Double totalTime = 0.0;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private List<TimelineEntry> timeline = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderedSpot {
        private Long spotId;
        private Long destinationId;
        private String spotName;
        private Double latitude;
        private Double longitude;
        private String transportMode;
        private Integer orderIndex;
        private Boolean routable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry {
        private String label;
        private String type;
        private String fromSpotName;
        private String toSpotName;
        private Double distance;
        private Double duration;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
