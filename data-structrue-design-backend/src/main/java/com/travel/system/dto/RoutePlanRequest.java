package com.travel.system.dto;

import lombok.Data;

@Data
public class RoutePlanRequest {
    private Long fromNodeId;
    private Long toNodeId;
    private String strategy;
    private String transport;
}
