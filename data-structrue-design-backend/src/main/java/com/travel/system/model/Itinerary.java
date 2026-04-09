package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String owner;
    private String collaborators;
    private String strategy;
    private String transportMode;
    @Column(length = 2000)
    private String notes;
    private LocalDateTime updatedAt;
}
