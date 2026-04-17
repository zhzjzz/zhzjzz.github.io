package com.travel.system.dto;

import com.travel.system.model.Facility;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FacilityQueryResult {
    private Facility facility;
    private Double distanceMeters;
}
