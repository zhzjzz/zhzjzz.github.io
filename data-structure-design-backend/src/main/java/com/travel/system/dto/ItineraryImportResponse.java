package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryImportResponse {
    private String title;
    private String summary;
    private String sourceType;
    private List<MatchedSpot> spots = new ArrayList<>();
    private List<UnmatchedSpot> unmatchedSpots = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedSpot {
        private String rawName;
        private Long matchedDestinationId;
        private String matchedName;
        private Double latitude;
        private Double longitude;
        private Integer dayIndex;
        private Integer orderIndex;
        private Integer stayMinutes;
        private Double confidence;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedSpot {
        private String rawName;
        private Integer dayIndex;
        private Integer orderIndex;
        private String reason;
    }
}
