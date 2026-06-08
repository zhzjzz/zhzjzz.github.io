package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorNavigationResponse {
    private String buildingName;
    private List<String> path;
    private List<String> steps;
    private double distanceMeters;
}
