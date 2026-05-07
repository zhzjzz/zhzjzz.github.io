package com.travel.system.model.nav;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityRoute {
    private Long routeId;
    private String fromSpot;
    private String toSpot;
    private String transitType;
    private Double timeCost;
    private Double distance;
}
