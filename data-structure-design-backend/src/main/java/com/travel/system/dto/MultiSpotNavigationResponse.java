package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiSpotNavigationResponse {
    private List<RouteSegment> segments;
    private Double totalDistance;
    private Double totalTime;
    private InnovationSummary innovationSummary;

    public MultiSpotNavigationResponse(List<RouteSegment> segments, Double totalDistance, Double totalTime) {
        this.segments = segments;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteSegment {
        private String type;
        private String fromSpotName;
        private String toSpotName;
        private Long fromNodeId;
        private Long toNodeId;
        private String transportMode;
        private String transitType;
        private List<double[]> path;
        private double[] cityTransitStart;
        private double[] cityTransitEnd;
        private Double distance;
        private Double time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InnovationSummary {
        private String travelerProfile;
        private Boolean optimizedVisitOrder;
        private Double originalCost;
        private Double optimizedCost;
        private Double savedCost;
        private List<String> explanations;
    }
}
