package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OsmRouteResponse {
    /** 路径总距离，单位：米。 */
    private Double distance;
    /** 预计通行耗时，单位：毫秒。 */
    private Long time;
    private List<double[]> path;
}
