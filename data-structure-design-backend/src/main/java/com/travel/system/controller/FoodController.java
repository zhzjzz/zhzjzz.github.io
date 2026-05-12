package com.travel.system.controller;

import com.travel.system.model.Food;
import com.travel.system.service.FoodService;
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
@RequestMapping("/api/foods")
@Tag(name = "美食管理", description = "美食查询、推荐等相关接口")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @Operation(summary = "搜索美食", description = "支持名称、菜系、店名、目的地、排序和数量限制")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping
    public List<Food> search(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "菜系") @RequestParam(required = false) String cuisine,
            @Parameter(description = "目的地 ID") @RequestParam(required = false) Long destinationId,
            @Parameter(description = "排序方式：recommend/rating/destinationHeat") @RequestParam(defaultValue = "recommend") String sort,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "30") int limit) {
        return foodService.search(keyword, cuisine, destinationId, sort, limit);
    }

    @Operation(summary = "美食菜系列表", description = "返回当前数据中的可选菜系")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/cuisines")
    public List<String> cuisines() {
        return foodService.cuisines();
    }

    @Operation(summary = "热门美食推荐", description = "根据热度和评分综合排序返回 Top-K 美食")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/top")
    public List<Food> top(
            @Parameter(description = "返回数量，默认为 10") @RequestParam(defaultValue = "10") int k) {
        return foodService.topK(k);
    }
}
