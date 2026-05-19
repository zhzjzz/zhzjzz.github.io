package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedGuidePlan {
    private String title;
    private String summary;
    private List<Place> places = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        private String name;
        private Integer dayIndex;
        private Integer orderIndex;
        private Integer stayMinutes;
        private String reason;
        private Double confidence;
    }
}
