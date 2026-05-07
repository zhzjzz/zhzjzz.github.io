package com.travel.system.model.nav;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadEdge {
    private Long u;
    private Long v;
    private String spotName;
    private Double length;
    private Double congestionBase;
    private String allowedVehicles;
    private byte[] geometry;
}
