package com.travel.system.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

@Data
@Document(indexName = "travel_itinerary")
public class ItineraryDocument {
    @Id
    private String id;
    private String name;
    private String owner;
    private String collaborators;
    private String strategy;
    private String transportMode;
    private String notes;
    private LocalDateTime updatedAt;
}
