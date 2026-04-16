package com.travel.system.controller;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.service.FacilitySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code FacilityController} 负责处理设施相关的 HTTP 请求。
 *
 * <p>提供以下功能：
 *
 * <ul>
 *   <li>查询设施列表并支持类型模糊搜索；</li>
 *   <li>基于位置和交通方式的附近设施搜索，返回按距离排序的结果。</li>
 * </ul>
 *
 * <p>该控制器结合 {@link FacilityMapper} 进行基础查询，
 * 并通过 {@link FacilitySearchService} 实现基于图结构的复杂空间检索。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/facilities")
@Tag(name = "设施管理", description = "设施查询、附近搜索等相关接口")
public class FacilityController {

    /**
     * {@link FacilityMapper} 持久层接口
     */
    private final FacilityMapper facilityMapper;

    /**
     * 设施搜索服务，提供基于图结构的空间查询能力。
     */
    private final FacilitySearchService facilitySearchService;

    /**
     * 构造函数注入依赖。
     *
     * @param facilityRepository    设施数据访问层
     * @param facilitySearchService 设施搜索服务
     */
    public FacilityController(FacilityMapper facilityRepository,
                              FacilitySearchService facilitySearchService) {
        this.facilityMapper = facilityRepository;
        this.facilitySearchService = facilitySearchService;
    }

    /**
     * 查询设施列表。
     *
     * @param type 可选的设施类型关键字；若为 {@code null} 或空字符串，则返回全部设施
     * @return 符合条件的 {@link Facility} 列表
     */
    @Operation(summary = "查询设施列表", description = "支持设施类型模糊搜索，无关键字则返回所有设施")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Facility> list(
            @Parameter(description = "设施类型关键字，用于模糊匹配") @RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) {
            // 未指定类型，返回所有设施记录
            return facilityMapper.findAll();
        }
        // 在设施类型字段进行模糊匹配（不区分大小写）
        return facilityMapper.findByFacilityTypeContainingIgnoreCase(type);
    }

    /**
     * 附近设施搜索接口。
     *
     * <p>从指定的道路节点出发，根据交通方式（步行/骑行/驾车）计算可达设施，
     * 并按距离由近到远排序返回。支持按类型和关键字过滤，以及最大距离限制。
     *
     * @param fromNodeId        起始道路节点 ID（必填）
     * @param type              设施类型过滤条件（可选）
     * @param keyword           名称或描述关键字过滤（可选）
     * @param maxDistanceMeters 最大搜索距离（米），为空则不限（可选）
     * @param transport         交通方式，支持 {@code walk}、{@code bike}、{@code drive}，默认 {@code walk}
     * @return 按距离排序的 {@link FacilityQueryResult} 列表
     */
    @Operation(summary = "附近设施搜索", description = "从指定道路节点出发，按交通方式搜索附近设施并排序")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping("/nearby")
    public List<FacilityQueryResult> nearby(
            @Parameter(description = "起始道路节点ID") @RequestParam Long fromNodeId,
            @Parameter(description = "设施类型过滤") @RequestParam(required = false) String type,
            @Parameter(description = "名称或描述关键字过滤") @RequestParam(required = false) String keyword,
            @Parameter(description = "最大搜索距离（米）") @RequestParam(required = false) Double maxDistanceMeters,
            @Parameter(description = "交通方式：walk/bike/drive，默认walk") @RequestParam(defaultValue = "walk") String transport) {
        // 委托 FacilitySearchService 执行基于图遍历的空间查询
        return facilitySearchService.searchNearby(fromNodeId, type, keyword, maxDistanceMeters, transport);
    }
}
