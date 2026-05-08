package com.travel.system.controller.nav;

import com.travel.system.model.nav.Poi;
import com.travel.system.service.nav.PoiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nav/pois")
@Tag(name = "服务设施查询", description = "洗手间、食堂、商店等 POI 查询接口")
public class PoiController {

    private final PoiService poiService;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }
    /**
     * 按前端传入的关键词、类型、分类或排序条件检索数据，返回可直接展示的候选列表。
     */

    @Operation(summary = "搜索服务设施")
    @GetMapping
    public List<Poi> search(
            @RequestParam(required = false) String spotName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return poiService.searchBySpotAndType(spotName, type, keyword);
    }
    /**
     * 根据主键 ID 查询单条数据，找不到时返回空结果，供详情页或关联查询使用。
     */

    @Operation(summary = "获取设施详情")
    @GetMapping("/{poiId}")
    public Poi getById(@PathVariable Long poiId) {
        return poiService.getById(poiId);
    }
    /**
     * 根据路网节点 ID 查询最近的建筑物或 POI，用于把导航节点映射成前端可读的地点信息。
     */

    @Operation(summary = "根据节点ID查找最近设施")
    @GetMapping("/nearby-node")
    public Poi getByNearestNode(@RequestParam Long nodeId) {
        return poiService.getByNearestNodeId(nodeId);
    }
    /**
     * 查询指定景区下的全部建筑物或 POI，供前端地点选择列表使用。
     */

    @Operation(summary = "获取某景区全部设施")
    @GetMapping("/by-spot")
    public List<Poi> listBySpot(@RequestParam String spotName) {
        return poiService.listBySpot(spotName);
    }
}
