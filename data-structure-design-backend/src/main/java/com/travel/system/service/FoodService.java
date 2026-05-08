package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.model.Food;
import com.travel.system.mapper.FoodMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodService {

    private final FoodMapper foodRepository;
    private final RecommendationService recommendationService;

    public FoodService(FoodMapper foodRepository,
                       RecommendationService recommendationService) {
        this.foodRepository = foodRepository;
        this.recommendationService = recommendationService;
    }

    /**

     * 按关键词、分类、排序字段和数量限制查询景区数据，供景区检索和推荐页面使用。

     */
    public List<Food> search(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return foodRepository.findAll();
        }
        return foodRepository.findByKeyword(keyword);
    }

    /**

     * 按默认或指定推荐策略返回前 k 条数据，k 非法时由 service 内部修正为安全默认值。

     */
    public List<Food> topK(int k) {
        List<Food> all = foodRepository.findAll();
        return recommendationService.topKFood(all, k);
    }

    /**

     * 保存或更新实体数据，并返回数据库持久化后的结果。

     */
    public Food save(Food food) {
        return foodRepository.save(food);
    }
}
