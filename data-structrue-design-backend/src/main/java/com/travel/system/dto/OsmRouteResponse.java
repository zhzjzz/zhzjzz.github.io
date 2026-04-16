package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OsmRouteResponse {
    private Double distance;
    private Long time;
    private List<double[]> path;
}
