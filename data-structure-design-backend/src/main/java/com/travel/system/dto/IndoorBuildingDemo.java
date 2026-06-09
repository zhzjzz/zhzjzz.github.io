package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorBuildingDemo {
    private String id;
    private String name;
    private String type;
    private String scenario;
    private List<Integer> floors;
    private String defaultFromNodeId;
    private String defaultToNodeId;
    private List<IndoorPoint> points;
    private List<IndoorConnection> connections;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndoorPoint {
        private String id;
        private String name;
        private Integer floor;
        private String type;
        private Double x;
        private Double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndoorConnection {
        private String fromNodeId;
        private String toNodeId;
        private String action;
    }
}
