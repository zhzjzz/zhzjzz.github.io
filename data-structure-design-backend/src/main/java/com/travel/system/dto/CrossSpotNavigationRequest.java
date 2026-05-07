package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrossSpotNavigationRequest {
    private String fromSpotName;
    private Long fromNodeId;
    private String toSpotName;
    private Long toNodeId;
    private String strategy;
    private String transportMode;
}
