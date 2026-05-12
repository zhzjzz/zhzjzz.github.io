package com.travel.system.service;

import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceFoodTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void topKFoodBoundsInvalidKAndOrdersByRecommendationScore() {
        Destination destination = new Destination();
        destination.setHeat(80d);

        Food strong = food("老北京炸酱面", 4.7, 96d, destination);
        Food weak = food("普通套餐", 3.9, 30d, destination);
        Food medium = food("湖畔拿铁", 4.5, 70d, destination);

        List<Food> results = service.topKFood(List.of(weak, medium, strong), -1);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(Food::getName)
                .containsExactly("老北京炸酱面", "湖畔拿铁", "普通套餐");
    }

    private Food food(String name, Double rating, Double heat, Destination destination) {
        Food food = new Food();
        food.setName(name);
        food.setRating(rating);
        food.setHeat(heat);
        food.setDestination(destination);
        return food;
    }
}
