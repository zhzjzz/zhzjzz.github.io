package com.travel.system.model.nav;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadNode {
    private Long osmid;
    private String spotName;
    private Double y;
    private Double x;
    private Integer floor;
    private byte[] geometry;
}
