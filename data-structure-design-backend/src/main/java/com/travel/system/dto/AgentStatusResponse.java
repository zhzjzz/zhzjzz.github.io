package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusResponse {
    private String provider;
    private boolean configured;
    private boolean apiKeyConfigured;
    private boolean modelConfigured;
    private String baseUrl;
    private String model;
}
