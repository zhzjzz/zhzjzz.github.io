package com.travel.system.model;

import com.travel.system.search.DestinationDocument;
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
    @Transient
    private Double heat;
    private Double rating;
    @Column(length = 1000)
    private String description;
    private Double latitude;
    private Double longitude;
    public Destination(DestinationDocument doc) {
        this.id = Long.valueOf(doc.getId());
        this.name = doc.getName();
        this.sceneType = doc.getSceneType();
        this.category = doc.getCategory();
        this.heat = doc.getHeat();
        this.rating = doc.getRating();
        this.description = doc.getDescription();
        this.latitude = doc.getLatitude();
        this.longitude = doc.getLongitude();
    }


}
