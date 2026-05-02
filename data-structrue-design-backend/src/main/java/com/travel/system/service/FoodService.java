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

    public List<Food> search(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return foodRepository.findAll();
        }
        return foodRepository.findByKeyword(keyword);
    }

    public List<Food> topK(int k) {
        List<Food> all = foodRepository.findAll();
        return recommendationService.topKFood(all, k);
    }

    public Food save(Food food) {
        return foodRepository.save(food);
    }
}
