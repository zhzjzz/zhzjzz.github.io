package com.travel.system.controller;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.model.Facility;
import com.travel.system.service.FacilitySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@Tag(name = "设施管理", description = "设施查询、附近搜索等相关接口")
public class FacilityController {

    private final FacilityMapper facilityMapper;
    private final FacilitySearchService facilitySearchService;

    public FacilityController(FacilityMapper facilityMapper,
                              FacilitySearchService facilitySearchService) {
        this.facilityMapper = facilityMapper;
        this.facilitySearchService = facilitySearchService;
    }

    @Operation(summary = "查询设施列表", description = "支持设施类型模糊搜索，无关键字则返回所有设施")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Facility> list(
            @Parameter(description = "设施类型关键字，用于模糊匹配") @RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) {
            return facilityMapper.findAll();
        }
        return facilityMapper.findByFacilityTypeContainingIgnoreCase(type);
    }

    @Operation(summary = "查询设施类别候选项", description = "返回去重后的设施类别，用于前端下拉搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/types")
    public List<String> types(
            @Parameter(description = "类别关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "目的地类型") @RequestParam(required = false) String sceneType,
            @Parameter(description = "最大返回数量，默认50，最大200") @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return facilityMapper.findDistinctFacilityTypes(normalizedKeyword, 200).stream()
                .filter(type -> FacilitySearchService.isVisibleFacilityTypeForScene(type, sceneType))
                .limit(safeLimit)
                .toList();
    }

    @Operation(summary = "附近设施搜索", description = "从指定经纬度出发搜索附近设施并按距离排序")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping("/nearby")
    public List<FacilityQueryResult> nearby(
            @Parameter(description = "起点纬度") @RequestParam Double fromLat,
            @Parameter(description = "起点经度") @RequestParam Double fromLon,
            @Parameter(description = "设施类型过滤") @RequestParam(required = false) String type,
            @Parameter(description = "名称或描述关键字过滤") @RequestParam(required = false) String keyword,
            @Parameter(description = "最大搜索距离，单位米") @RequestParam(required = false) Double maxDistanceMeters,
            @Parameter(description = "目的地名称") @RequestParam(required = false) String spotName,
            @Parameter(description = "目的地类型") @RequestParam(required = false) String sceneType) {
        return facilitySearchService.searchNearby(fromLat, fromLon, type, keyword, maxDistanceMeters, spotName, sceneType);
    }
}
