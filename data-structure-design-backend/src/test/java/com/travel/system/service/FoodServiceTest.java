package com.travel.system.service;

import com.travel.system.mapper.FoodMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodServiceTest {

    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final RecommendationService recommendationService = new RecommendationService();
    private final FoodService service = new FoodService(foodMapper, recommendationService);

    @Test
    void searchFiltersKeywordCuisineDestinationAndSortsByRating() {
        Destination campus = destination(1L, "北京邮电大学", 88d);
        Destination park = destination(2L, "紫竹院公园", 72d);
        Food noodles = food(1L, "老北京炸酱面", "京菜", "校园食堂一层", 4.6, 91d, campus);
        Food coffee = food(2L, "湖畔拿铁", "咖啡", "湖畔咖啡", 4.8, 66d, park);
        Food rice = food(3L, "招牌鸡腿饭", "快餐", "学苑餐厅", 4.4, 80d, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, coffee, rice));

        List<Food> results = service.search("饭", "快餐", 1L, "rating", 10);

        assertThat(results).extracting(Food::getName).containsExactly("招牌鸡腿饭");
    }

    @Test
    void searchUsesRecommendationSortAndLimitByDefault() {
        Destination campus = destination(1L, "北京邮电大学", 88d);
        Food noodles = food(1L, "老北京炸酱面", "京菜", "校园食堂一层", 4.6, 91d, campus);
        Food burger = food(2L, "牛肉芝士堡", "西式简餐", "学生活动中心轻食", 4.2, 95d, campus);
        Food dessert = food(3L, "桂花酒酿圆子", "甜品", "校园甜品铺", 4.9, 70d, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, burger, dessert));

        List<Food> results = service.search(null, null, null, "recommend", 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("老北京炸酱面");
    }

    private Destination destination(Long id, String name, Double heat) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setHeat(heat);
        return destination;
    }

    private Food food(Long id,
                      String name,
                      String cuisine,
                      String storeName,
                      Double rating,
                      Double heat,
                      Destination destination) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCuisine(cuisine);
        food.setStoreName(storeName);
        food.setRating(rating);
        food.setHeat(heat);
        food.setDestination(destination);
        return food;
    }
}
