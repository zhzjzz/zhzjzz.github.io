package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT")
    private Long id;
    private String name;
    private String cuisine;
    private String storeName;
    private Double rating;
    private Double heat;
    @Transient
    private Double distanceMeters;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Destination destination;
}
