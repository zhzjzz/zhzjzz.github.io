package com.travel.system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String sceneType;
    private String category;
    private Double heat;
    private Double rating;
    @Column(length = 1000)
    private String description;
    private Double latitude;
    private Double longitude;
}
