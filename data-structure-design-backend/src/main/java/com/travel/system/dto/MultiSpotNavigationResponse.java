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
}
