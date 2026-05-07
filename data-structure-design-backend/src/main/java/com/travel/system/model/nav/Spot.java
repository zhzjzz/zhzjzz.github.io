package com.travel.system.model.nav;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Spot {
    private Long spotId;
    private String name;
    private String category;
    private Integer hotness;
    private Double rating;
    private String description;
    private String keywords;
}
