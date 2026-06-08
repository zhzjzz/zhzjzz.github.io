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
    private Long startNodeId;
    private Boolean returnToStart;
    private List<SpotVisit> visits;

    public MultiSpotNavigationRequest(String strategy, Boolean optimizeVisitOrder, List<SpotVisit> visits) {
        this.strategy = strategy;
        this.optimizeVisitOrder = optimizeVisitOrder;
        this.visits = visits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotVisit {
        private String spotName;
        private List<Long> nodeIds;
        private String transportMode;
    }
}
