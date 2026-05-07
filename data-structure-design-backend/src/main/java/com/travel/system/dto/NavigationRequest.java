package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationRequest {
    private Long startNodeId;
    private Long endNodeId;
    private String spotName;
    private String strategy;
    private String transportMode;
    private List<String> mixedTransportModes;
}
