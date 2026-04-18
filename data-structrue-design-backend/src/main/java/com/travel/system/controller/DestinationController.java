package com.travel.system.controller;

import com.travel.system.model.Destination;
import com.travel.system.service.DestinationService;
import com.travel.system.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code DestinationController} 负责处理与景区/校园目的地相关的 HTTP 请求。
 *
 * <p>提供以下功能：
 *
 * <ul>
 *   <li>查询目的地列表并支持关键字模糊搜索（优先使用 Elasticsearch）；</li>
 *   <li>基于热度和评分的 Top‑K 推荐接口；</li>
 *   <li>新增目的地数据的持久化并同步到 Elasticsearch。</li>
 * </ul>
 *
 * 该控制器使用 Spring MVC 注解实现 RESTful 风格的 API，所有路径统一以 {@code /api/destinations}
 * 为前缀。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/destinations")
@Tag(name = "目的地管理", description = "目的地查询、推荐、新增等相关接口")
public class DestinationController {

    /** 目的地业务服务，负责持久化与搜索的协调（支持 Elasticsearch）。 */
    private final DestinationService destinationService;

    /* 用于业务层推荐逻辑的服务。 */
    //private final RecommendationService recommendationService;

    /**
     * 构造函数注入所需的依赖。
     *
     * @param destinationService    目的地业务服务（支持 ES 搜索）
     * //@param recommendationService  业务层推荐服务
     */
    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
        //this.recommendationService = recommendationService;
    }

    /**
     * 查询目的地列表。
     *
     * <p>优先使用 Elasticsearch 进行模糊搜索，若 ES 不可用则回退到 MySQL 查询。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部目的地
     * @return 符合条件的 {@link Destination} 列表
     */
    @Operation(summary = "查询目的地列表", description = "支持关键字模糊搜索（优先使用 Elasticsearch），无关键字则返回所有目的地")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Destination> list(
            @Parameter(description = "搜索关键字，用于模糊匹配名称或类别") @RequestParam(required = false) String keyword) {
        // 通过 Service 层处理搜索逻辑，优先使用 ES
        return destinationService.list(keyword, 1, Integer.MAX_VALUE);
    }

    @Operation(summary = "路线规划地点搜索", description = "用于路线规划输入框的地点搜索，优先 Elasticsearch，支持限制返回数量")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/route-search")
    public List<Destination> routeSearch(
            @Parameter(description = "搜索关键字，用于模糊匹配名称或类别") @RequestParam String keyword,
            @Parameter(description = "最大返回数量，默认10，最大50") @RequestParam(defaultValue = "10") int limit) {
        return destinationService.searchForRoute(keyword, limit);
    }

    /**
     * 返回热度+评分综合排序的前 {@code k} 名目的地。
     *
     * @param k 想要返回的目的地数量，默认值为 10
     * @return 已排序的 {@link Destination} 列表
     */
    @Operation(summary = "热门目的地推荐", description = "根据热度和评分综合排序返回Top-K目的地")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/top")
    public List<Destination> top(
            @Parameter(description = "返回数量，默认为10") @RequestParam(defaultValue = "10") int k) {
        // 使用 Service 层获取 Top-K 推荐
        return destinationService.topK(k);
    }

    /**
     * 新增目的地记录并同步到 Elasticsearch。
     *
     * @param destination 前端提交的目的地实体（JSON → {@link Destination}）
     * @return 保存后的实体，包含数据库生成的主键等信息
     */
    @Operation(summary = "新增目的地", description = "创建新的目的地记录并同步到 Elasticsearch")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        // 通过 Service 层保存，确保同步到 Elasticsearch
        return destinationService.save(destination);
    }
}
