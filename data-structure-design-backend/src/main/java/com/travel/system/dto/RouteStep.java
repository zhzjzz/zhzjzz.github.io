package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStep {
    private Long fromNodeId;
    private Long toNodeId;
    private String transportMode;
    private Double distance;
    private Double time;
    private Double congestion;
}
