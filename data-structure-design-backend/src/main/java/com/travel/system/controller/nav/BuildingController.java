package com.travel.system.controller.nav;

import com.travel.system.model.nav.Building;
import com.travel.system.service.nav.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nav/buildings")
@Tag(name = "建筑物查询", description = "教学楼、办公楼、宿舍楼等建筑物查询接口")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @Operation(summary = "搜索建筑物")
    @GetMapping
    public List<Building> search(
            @RequestParam(required = false) String spotName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return buildingService.searchBySpotAndType(spotName, type, keyword);
    }

    @Operation(summary = "获取建筑物详情")
    @GetMapping("/{buildingId}")
    public Building getById(@PathVariable Long buildingId) {
        return buildingService.getById(buildingId);
    }

    @Operation(summary = "根据节点ID查找最近建筑物")
    @GetMapping("/nearby-node")
    public Building getByNearestNode(@RequestParam Long nodeId) {
        return buildingService.getByNearestNodeId(nodeId);
    }

    @Operation(summary = "获取某景区全部建筑物")
    @GetMapping("/by-spot")
    public List<Building> listBySpot(@RequestParam String spotName) {
        return buildingService.listBySpot(spotName);
    }
}
