package com.travel.system.model.nav;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    private Long buildingId;
    private String spotName;
    private String name;
    private String type;
    private Long nearestNodeId;
}
