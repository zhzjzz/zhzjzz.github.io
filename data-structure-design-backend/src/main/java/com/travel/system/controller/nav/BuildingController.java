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
    /**
     * 按前端传入的关键词、类型、分类或排序条件检索数据，返回可直接展示的候选列表。
     */

    @Operation(summary = "搜索建筑物")
    @GetMapping
    public List<Building> search(
            @RequestParam(required = false) String spotName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return buildingService.searchBySpotAndType(spotName, type, keyword);
    }
    /**
     * 根据主键 ID 查询单条数据，找不到时返回空结果，供详情页或关联查询使用。
     */

    @Operation(summary = "获取建筑物详情")
    @GetMapping("/{buildingId}")
    public Building getById(@PathVariable Long buildingId) {
        return buildingService.getById(buildingId);
    }
    /**
     * 根据路网节点 ID 查询最近的建筑物或 POI，用于把导航节点映射成前端可读的地点信息。
     */

    @Operation(summary = "根据节点ID查找最近建筑物")
    @GetMapping("/nearby-node")
    public Building getByNearestNode(@RequestParam Long nodeId) {
        return buildingService.getByNearestNodeId(nodeId);
    }
    /**
     * 查询指定景区下的全部建筑物或 POI，供前端地点选择列表使用。
     */

    @Operation(summary = "获取某景区全部建筑物")
    @GetMapping("/by-spot")
    public List<Building> listBySpot(@RequestParam String spotName) {
        return buildingService.listBySpot(spotName);
    }
}
