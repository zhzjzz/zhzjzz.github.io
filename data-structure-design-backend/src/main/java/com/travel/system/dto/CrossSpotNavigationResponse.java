package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrossSpotNavigationResponse {

    private List<double[]> microPathStart;
    private Double segment1Distance;
    private Double segment1Time;

    private double[] cityTransitStart;
    private double[] cityTransitEnd;
    private String transitType;
    private Double segment2Distance;
    private Double segment2Time;

    private List<double[]> microPathEnd;
    private Double segment3Distance;
    private Double segment3Time;

    private Double totalDistance;
    private Double totalTime;
}
