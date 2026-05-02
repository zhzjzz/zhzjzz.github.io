package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryBroadcastMessage {
    public enum Type { UPDATED, CONFLICT }

    private Type type;
    private String username;
    private Object itinerary;
    private String message;
    private LocalDateTime serverTimestamp;
}
