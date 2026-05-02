package com.travel.system.dto;

import lombok.Data;

@Data
public class ItineraryEditMessage {
    private Long itineraryId;
    private String username;
    private String expectedUpdatedAt;
    private String name;
    private String strategy;
    private String transportMode;
    private String notes;
}
