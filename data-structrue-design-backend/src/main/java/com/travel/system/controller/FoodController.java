package com.travel.system.controller;

import com.travel.system.model.Food;
import com.travel.system.repository.FoodRepository;
import com.travel.system.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodRepository foodRepository;
    private final RecommendationService recommendationService;

    public FoodController(FoodRepository foodRepository, RecommendationService recommendationService) {
        this.foodRepository = foodRepository;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<Food> search(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return foodRepository.findAll();
        }
        return foodRepository.findByNameContainingIgnoreCaseOrCuisineContainingIgnoreCaseOrStoreNameContainingIgnoreCase(
                keyword, keyword, keyword);
    }

    @GetMapping("/top")
    public List<Food> top(@RequestParam(defaultValue = "10") int k) {
        return recommendationService.topKFood(foodRepository.findAll(), k);
    }
}
