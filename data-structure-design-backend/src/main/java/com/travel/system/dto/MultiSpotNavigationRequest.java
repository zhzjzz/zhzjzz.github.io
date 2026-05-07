package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiSpotNavigationRequest {
    private String strategy;
    private Boolean optimizeVisitOrder;
    private List<SpotVisit> visits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotVisit {
        private String spotName;
        private List<Long> nodeIds;
        private String transportMode;
    }
}
