package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class RoadNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String nodeType;
    private Double latitude;
    private Double longitude;
}
