package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorNavigationResponse {
    private String buildingId;
    private String buildingName;
    private String fromNodeId;
    private String fromName;
    private String toNodeId;
    private String toName;
    private Double totalDistance;
    private List<IndoorRouteStep> steps;
    private List<IndoorFloorSegment> floorSegments;
    private List<String> notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndoorRouteStep {
        private String fromNodeId;
        private String fromName;
        private String toNodeId;
        private String toName;
        private Integer fromFloor;
        private Integer toFloor;
        private String action;
        private String instruction;
        private Double distance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndoorFloorSegment {
        private Integer floor;
        private String title;
        private List<String> pointNames;
        private Double distance;
    }
}
