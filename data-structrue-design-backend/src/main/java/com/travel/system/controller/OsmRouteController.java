package com.travel.system.controller;

import com.travel.system.dto.OsmRouteResponse;
import com.travel.system.service.OsmRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "OSM 导航", description = "基于 GraphHopper + OpenStreetMap 的经纬度路线规划接口")
public class OsmRouteController {
    private final OsmRouteService osmRouteService;

    public OsmRouteController(OsmRouteService osmRouteService) {
        this.osmRouteService = osmRouteService;
    }

    @Operation(summary = "经纬度路线规划", description = "输入起终点经纬度，返回路线点集、总距离和预计耗时")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "规划成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或路径不可达"),
            @ApiResponse(responseCode = "503", description = "GraphHopper 未启用或未加载")
    })
    @GetMapping("/route")
    public OsmRouteResponse route(@RequestParam double startLat,
                                  @RequestParam double startLon,
                                  @RequestParam double endLat,
                                  @RequestParam double endLon) {
        return osmRouteService.route(startLat, startLon, endLat, endLon);
    }
}
