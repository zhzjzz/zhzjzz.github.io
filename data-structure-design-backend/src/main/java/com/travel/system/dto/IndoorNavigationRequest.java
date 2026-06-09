package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorNavigationRequest {
    private String buildingId;
    private String fromNodeId;
    private String toNodeId;
    private String strategy;
}
