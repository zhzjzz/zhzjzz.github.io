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

    @Operation(summary = "搜索服务设施")
    @GetMapping
    public List<Poi> search(
            @RequestParam(required = false) String spotName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return poiService.searchBySpotAndType(spotName, type, keyword);
    }

    @Operation(summary = "获取设施详情")
    @GetMapping("/{poiId}")
    public Poi getById(@PathVariable Long poiId) {
        return poiService.getById(poiId);
    }

    @Operation(summary = "根据节点ID查找最近设施")
    @GetMapping("/nearby-node")
    public Poi getByNearestNode(@RequestParam Long nodeId) {
        return poiService.getByNearestNodeId(nodeId);
    }

    @Operation(summary = "获取某景区全部设施")
    @GetMapping("/by-spot")
    public List<Poi> listBySpot(@RequestParam String spotName) {
        return poiService.listBySpot(spotName);
    }
}
