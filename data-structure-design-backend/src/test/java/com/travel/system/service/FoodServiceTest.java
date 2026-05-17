package com.travel.system.service;

import com.travel.system.mapper.FoodMapper;
import com.travel.system.dto.FoodPlaceAnchor;
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
        Destination campus = destination(1L, "BUPT", 88d, 39.9652, 116.3511);
        Destination park = destination(2L, "Zizhuyuan Park", 72d, 39.9444, 116.3192);
        Food noodles = food(1L, "Beijing noodles", "Jing cuisine", "Campus canteen", 4.6, 91d, null, null, campus);
        Food coffee = food(2L, "Lakeside latte", "Coffee", "Lake cafe", 4.8, 66d, null, null, park);
        Food rice = food(3L, "Chicken rice", "Fast food", "Student diner", 4.4, 80d, null, null, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, coffee, rice));

        List<Food> results = service.search("rice", "Fast food", 1L, "rating", 10);

        assertThat(results).extracting(Food::getName).containsExactly("Chicken rice");
    }

    @Test
    void searchUsesRecommendationSortAndLimitByDefault() {
        Destination campus = destination(1L, "BUPT", 88d, 39.9652, 116.3511);
        Food noodles = food(1L, "Beijing noodles", "Jing cuisine", "Campus canteen", 4.6, 91d, null, null, campus);
        Food burger = food(2L, "Beef burger", "Western", "Student center", 4.2, 95d, null, null, campus);
        Food dessert = food(3L, "Sweet dumplings", "Dessert", "Dessert shop", 4.9, 70d, null, null, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, burger, dessert));

        List<Food> results = service.search(null, null, null, "recommend", 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Beijing noodles");
    }

    @Test
    void searchNearKnownPlaceFiltersCuisineAndSortsByDistance() {
        Food qianmen = food(1L, "Luzhu", "Jing cuisine", "Qianmen old taste", 4.4, 90d,
                39.8994, 116.3976, destination(11L, "Dashilar", 80d, 39.8994, 116.3976));
        Food niujie = food(2L, "Halal snacks", "Halal", "Niujie snacks", 4.7, 94d,
                39.8867, 116.3596, destination(12L, "Niujie", 82d, 39.8867, 116.3596));
        Food campus = food(3L, "Campus noodles", "Jing cuisine", "Campus canteen", 4.6, 91d,
                39.9652, 116.3511, destination(13L, "BUPT", 88d, 39.9652, 116.3511));
        when(foodMapper.findAll()).thenReturn(List.of(campus, niujie, qianmen));

        List<Food> results = service.search(null, "Jing cuisine", null, "distance", 10,
                "Tiananmen", null, null, 3_000d);

        assertThat(results).extracting(Food::getName).containsExactly("Luzhu");
        assertThat(results.get(0).getDistanceMeters()).isBetween(900d, 1_300d);
    }

    @Test
    void searchNearbyPhraseUsesPlaceAsAnchorWithoutTreatingWholePhraseAsFoodKeyword() {
        Food qianmen = food(1L, "Luzhu", "Jing cuisine", "Qianmen old taste", 4.4, 90d,
                39.8994, 116.3976, destination(11L, "Dashilar", 80d, 39.8994, 116.3976));
        Food campus = food(2L, "Campus noodles", "Jing cuisine", "Campus canteen", 4.6, 91d,
                39.9652, 116.3511, destination(13L, "BUPT", 88d, 39.9652, 116.3511));
        when(foodMapper.findAll()).thenReturn(List.of(campus, qianmen));

        List<Food> results = service.search("Tiananmen nearby restaurants", null, null, "distance", 10);

        assertThat(results).extracting(Food::getName).containsExactly("Luzhu");
        assertThat(results.get(0).getDistanceMeters()).isNotNull();
    }

    @Test
    void searchCanResolveAnySpotAnchorFromDatabase() {
        Food restaurant = food(1L, "Palace noodles", "noodles", "Palace cafe", 4.5, 82d,
                39.9165, 116.3970, null);
        Food far = food(2L, "Far burger", "fast_food", "Far diner", 4.2, 70d,
                39.9652, 116.3511, null);
        when(foodMapper.findAll()).thenReturn(List.of(far, restaurant));
        when(foodMapper.findPlaceAnchors()).thenReturn(List.of(anchor("故宫博物院", 39.9163, 116.3972)));

        List<Food> results = service.search(null, null, null, "distance", 10,
                "故宫", null, null, 1_000d);

        assertThat(results).extracting(Food::getName).containsExactly("Palace noodles");
        assertThat(results.get(0).getDistanceMeters()).isLessThan(100d);
    }

    @Test
    void searchCuisineFilterMatchesFriendlyTypeLabels() {
        Food fastFood = food(1L, "Burger", "fast_food", "Burger shop", 4.3, 76d,
                39.9087, 116.3975, null);
        Food coffee = food(2L, "Latte", "coffee", "Coffee shop", 4.7, 80d,
                39.9088, 116.3976, null);
        when(foodMapper.findAll()).thenReturn(List.of(fastFood, coffee));

        List<Food> results = service.search(null, "快餐", null, "distance", 10,
                "天安门", null, null, 1_000d);

        assertThat(results).extracting(Food::getName).containsExactly("Burger");
    }

    @Test
    void cuisinesReturnsReadableDedupedTypeLabels() {
        when(foodMapper.findCuisines()).thenReturn(List.of("fast_food", "coffee", "咖啡", "bakery", "unknown", ""));

        List<String> results = service.cuisines();

        assertThat(results).containsExactly("咖啡", "快餐", "烘焙", "餐饮");
    }

    @Test
    void placeAnchorsReturnsDedupedSortedSearchableSpotNames() {
        when(foodMapper.findPlaceAnchors()).thenReturn(List.of(
                anchor("故宫博物院", 39.9163, 116.3972),
                anchor("故宫博物院", 39.9163, 116.3972),
                anchor("天坛公园", 39.8822, 116.4066),
                anchor("", 39.9, 116.4),
                anchor("No Location", null, 116.4)
        ));

        List<FoodPlaceAnchor> results = service.placeAnchors();

        assertThat(results).extracting(FoodPlaceAnchor::getName).containsExactly("天坛公园", "故宫博物院");
    }

    @Test
    void searchFiltersAveragePriceRangeAndSortsAscending() {
        Food cheap = food(1L, "Soy milk", "breakfast", "Morning stall", 4.2, 65d,
                39.9087, 116.3975, null);
        cheap.setAveragePrice(12d);
        Food mid = food(2L, "Noodles", "noodles", "Noodle shop", 4.6, 88d,
                39.9088, 116.3976, null);
        mid.setAveragePrice(38d);
        Food premium = food(3L, "Roast duck", "beijing", "Duck house", 4.8, 94d,
                39.9089, 116.3977, null);
        premium.setAveragePrice(168d);
        when(foodMapper.findAll()).thenReturn(List.of(premium, cheap, mid));

        List<Food> results = service.search(null, null, null, "averagePrice", 10,
                "天安门", null, null, 1_000d, 20d, 100d);

        assertThat(results).extracting(Food::getName).containsExactly("Noodles");
        assertThat(results.get(0).getAveragePrice()).isEqualTo(38d);
    }

    private Destination destination(Long id, String name, Double heat, Double latitude, Double longitude) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setHeat(heat);
        destination.setLatitude(latitude);
        destination.setLongitude(longitude);
        return destination;
    }

    private Food food(Long id,
                      String name,
                      String cuisine,
                      String storeName,
                      Double rating,
                      Double heat,
                      Double latitude,
                      Double longitude,
                      Destination destination) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCuisine(cuisine);
        food.setStoreName(storeName);
        food.setRating(rating);
        food.setHeat(heat);
        food.setLatitude(latitude);
        food.setLongitude(longitude);
        food.setDestination(destination);
        return food;
    }

    private FoodPlaceAnchor anchor(String name, Double latitude, Double longitude) {
        FoodPlaceAnchor anchor = new FoodPlaceAnchor();
        anchor.setName(name);
        anchor.setLatitude(latitude);
        anchor.setLongitude(longitude);
        return anchor;
    }
}
