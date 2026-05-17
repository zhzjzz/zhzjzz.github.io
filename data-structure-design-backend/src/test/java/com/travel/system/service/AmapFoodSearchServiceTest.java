package com.travel.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.mapper.FoodMapper;
import com.travel.system.model.Food;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AmapFoodSearchServiceTest {

    private final AmapFoodSearchService service = new AmapFoodSearchService(
            mock(FoodMapper.class),
            new ObjectMapper(),
            "",
            "北京"
    );

    @Test
    void parsesAmapAroundSearchPoisWithCostRatingAddressAndDistance() throws Exception {
        String json = """
                {
                  "status": "1",
                  "pois": [
                    {
                      "id": "B000A8UIN8",
                      "name": "四季民福烤鸭店",
                      "type": "餐饮服务;中餐厅;北京菜",
                      "address": "南池子大街11号",
                      "location": "116.397800,39.908900",
                      "distance": "120",
                      "photos": [
                        {
                          "title": "storefront",
                          "url": "https://store.is.autonavi.com/showpic/example.jpg"
                        }
                      ],
                      "biz_ext": {
                        "rating": "4.7",
                        "cost": "142.00"
                      }
                    }
                  ]
                }
                """;

        List<Food> foods = service.parseAmapPois(json, 39.9087, 116.3975, 10);

        assertThat(foods).hasSize(1);
        Food food = foods.get(0);
        assertThat(food.getName()).isEqualTo("四季民福烤鸭店");
        assertThat(food.getStoreName()).isEqualTo("四季民福烤鸭店");
        assertThat(food.getCuisine()).isEqualTo("京菜");
        assertThat(food.getAddress()).isEqualTo("南池子大街11号");
        assertThat(food.getRating()).isEqualTo(4.7);
        assertThat(food.getAveragePrice()).isEqualTo(142d);
        assertThat(food.getImageUrl()).isEqualTo("https://store.is.autonavi.com/showpic/example.jpg");
        assertThat(food.getSourceType()).isEqualTo("amap-live");
        assertThat(food.getSourceId()).isEqualTo("B000A8UIN8");
        assertThat(food.getDistanceMeters()).isEqualTo(120d);
    }
}
