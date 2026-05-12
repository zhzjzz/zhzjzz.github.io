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

    public List<Food> search(String keyword, String cuisine, Long destinationId, String sort, int limit) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCuisine = normalize(cuisine);
        int safeLimit = limit <= 0 ? 30 : Math.min(limit, 100);
        List<Food> filtered = foodRepository.findAll().stream()
                .filter(food -> matchesKeyword(food, normalizedKeyword))
                .filter(food -> normalizedCuisine == null || normalizedCuisine.equalsIgnoreCase(normalize(food.getCuisine())))
                .filter(food -> destinationId == null || (food.getDestination() != null && destinationId.equals(food.getDestination().getId())))
                .toList();

        return sortFoods(filtered, sort).stream().limit(safeLimit).toList();
    }

    public List<String> cuisines() {
        return foodRepository.findCuisines();
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

    private boolean matchesKeyword(Food food, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(food.getName(), keyword)
                || contains(food.getCuisine(), keyword)
                || contains(food.getStoreName(), keyword)
                || (food.getDestination() != null && contains(food.getDestination().getName(), keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<Food> sortFoods(List<Food> foods, String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating())))
                    .toList();
        }
        if ("destinationHeat".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted((a, b) -> Double.compare(destinationHeat(b), destinationHeat(a)))
                    .toList();
        }
        return recommendationService.topKFood(foods, foods.size());
    }

    private double destinationHeat(Food food) {
        return food.getDestination() == null ? 0 : safe(food.getDestination().getHeat());
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }
}
