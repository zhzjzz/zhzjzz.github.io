package com.travel.system.controller;

import com.travel.system.model.Food;
import com.travel.system.mapper.FoodMapper;
import com.travel.system.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code FoodController} 负责处理美食（Food）相关的 HTTP 请求。
 *
 * <p>提供两类功能：
 *
 * <ul>
 *   <li>基于关键字的模糊搜索（在名称、菜系、门店名上同时匹配）；</li>
 *   <li>基于热度/评分的 Top‑K 推荐接口。</li>
 * </ul>
 *
 * <p>搜索逻辑直接委托给 {@link FoodRepository}，推荐逻辑统一交给 {@link RecommendationService} 实现。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/foods")
public class FoodController {

    /** 美食数据的 JPA 持久层仓库。 */
private final FoodMapper foodRepository;

    /** 推荐服务，用于计算热度/评分综合的 Top‑K 列表。 */
    private final RecommendationService recommendationService;

    /**
     * 构造函数注入依赖。
     *
     * @param foodRepository          美食数据访问层
     * @param recommendationService   推荐业务层
     */
public FoodController(FoodMapper foodRepository,
                          RecommendationService recommendationService) {
        this.foodRepository = foodRepository;
        this.recommendationService = recommendationService;
    }

    /**
     * 关键字搜索美食。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部美食
     * @return 匹配的 {@link Food} 列表
     */
    @GetMapping
    public List<Food> search(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 未提供关键字，返回所有记录
            return foodRepository.findAll();
        }
        // 在名称、菜系、店名三个字段进行不区分大小写的模糊匹配
        // 使用统一的关键字搜索方法
        return foodRepository.findByKeyword(keyword);
    }

    /**
     * 基于热度/评分的 Top‑K 推荐。
     *
     * @param k 想要返回的美食数量，默认 10
     * @return 已排序的 {@link Food} 列表
     */
    @GetMapping("/top")
    public List<Food> top(@RequestParam(defaultValue = "10") int k) {
        // 先读取所有美食，再交由推荐服务完成排序截取
        return recommendationService.topKFood(foodRepository.findAll(), k);
    }
}
