package com.travel.system.dto;

import lombok.Data;

@Data
public class ItineraryEditMessage {
    public enum Type { JOIN, LEAVE, EDITING, PATCH }

    private Long itineraryId;
    private Type type;
    private String username;
    private String expectedUpdatedAt;
    private String field;
    private String value;
    private String name;
    private String owner;
    private String collaborators;
    private String strategy;
    private String transportMode;
    private String notes;
}
