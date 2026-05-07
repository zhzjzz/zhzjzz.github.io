package com.travel.system.controller;

import com.travel.system.model.Food;
import com.travel.system.service.FoodService;
import com.travel.system.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code FoodController} 负责处理美食（Food）相关的 HTTP 请求。
 *
 * <p>提供两类功能：
 *
 * <ul>
 *   <li>基于关键字的模糊搜索（优先使用 Elasticsearch 在名称、菜系、门店名上同时匹配）；</li>
 *   <li>基于热度/评分的 Top‑K 推荐接口。</li>
 * </ul>
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/foods")
@Tag(name = "美食管理", description = "美食查询、推荐等相关接口")
public class FoodController {

    /** 美食业务服务，负责搜索与持久化（支持 Elasticsearch）。 */
    private final FoodService foodService;

    /** 推荐服务，用于计算热度/评分综合的 Top‑K 列表。 */
    private final RecommendationService recommendationService;

    /**
     * 构造函数注入依赖。
     *
     * @param foodService             美食业务服务
     * @param recommendationService   推荐业务层
     */
    public FoodController(FoodService foodService,
                          RecommendationService recommendationService) {
        this.foodService = foodService;
        this.recommendationService = recommendationService;
    }

    /**
     * 关键字搜索美食。
     *
     * <p>优先使用 Elasticsearch 进行模糊搜索，若 ES 不可用则回退到 MySQL 查询。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部美食
     * @return 匹配的 {@link Food} 列表
     */
    @Operation(summary = "搜索美食", description = "支持名称、菜系、店名关键字模糊搜索（优先使用 Elasticsearch）")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping
    public List<Food> search(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        // 通过 Service 层处理搜索，优先使用 ES
        return foodService.search(keyword, 1, Integer.MAX_VALUE);
    }

    /**
     * 基于热度/评分的 Top‑K 推荐。
     *
     * @param k 想要返回的美食数量，默认 10
     * @return 已排序的 {@link Food} 列表
     */
    @Operation(summary = "热门美食推荐", description = "根据热度和评分综合排序返回Top-K美食")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/top")
    public List<Food> top(
            @Parameter(description = "返回数量，默认为10") @RequestParam(defaultValue = "10") int k) {
        // 使用 Service 层获取 Top-K 推荐
        return foodService.topK(k);
    }
}
