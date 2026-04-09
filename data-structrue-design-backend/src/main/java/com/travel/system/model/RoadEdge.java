package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class RoadEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id")
    private RoadNode fromNode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id")
    private RoadNode toNode;
    private Double distanceMeters;
    private Double idealSpeed;
    private Double congestion;
    private String allowedTransport;
}
