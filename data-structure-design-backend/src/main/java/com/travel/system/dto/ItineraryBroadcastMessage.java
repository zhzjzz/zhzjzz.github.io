package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryBroadcastMessage {
    public enum Type { JOINED, LEFT, EDITING, UPDATED, CONFLICT }

    private Type type;
    private String username;
    private Object itinerary;
    private String message;
    private LocalDateTime serverTimestamp;
    private List<String> onlineUsers;
    private String field;
    private String value;

    public ItineraryBroadcastMessage(Type type,
                                     String username,
                                     Object itinerary,
                                     String message,
                                     LocalDateTime serverTimestamp) {
        this.type = type;
        this.username = username;
        this.itinerary = itinerary;
        this.message = message;
        this.serverTimestamp = serverTimestamp;
    }
}
