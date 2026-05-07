package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationResponse {
    private List<Long> path;
    private List<RouteStep> steps;
    private Double totalDistance;
    private Double totalTime;
}
