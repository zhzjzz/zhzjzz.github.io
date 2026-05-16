package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlannerPreviewRequest {
    private LocalDateTime departureTime;
    private String strategy;
    private Boolean optimizeVisitOrder;
    private List<PlannerSpot> spots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannerSpot {
        private Long spotId;
        private Long destinationId;
        private String spotName;
        private Double latitude;
        private Double longitude;
        private String transportMode;
        private Boolean selected;
        private Integer stayMinutes;
    }
}
