package com.travel.system.service;

import com.travel.system.model.Destination;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceDestinationTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void interestKeywordProducesVisibleTopOrderChange() {
        Destination city = destination("高热城市地标", "城市", "商业街区", "夜景购物", 100d, 5.0);
        Destination mountain = destination("山林自然步道", "景区", "自然", "森林和湖泊", 60d, 4.2);
        Destination museum = destination("历史博物馆", "景区", "历史", "古代文明展览", 65d, 4.3);

        List<Destination> defaultTop = service.topKDestinations(
                List.of(city, mountain, museum),
                3,
                RecommendationService.DestinationRankingMode.COMPOSITE);
        List<Destination> naturalTop = service.topKDestinations(
                List.of(city, mountain, museum),
                3,
                RecommendationService.DestinationRankingMode.COMPOSITE,
                "自然");

        assertThat(defaultTop).extracting(Destination::getName).startsWith("高热城市地标");
        assertThat(naturalTop).extracting(Destination::getName).startsWith("山林自然步道");
    }

    @Test
    void historyInterestMatchesRelatedCultureKeywords() {
        Destination city = destination("高热城市地标", "城市", "商业街区", "夜景购物", 100d, 5.0);
        Destination museum = destination("古代文明展馆", "景区", "博物馆", "文化展览", 65d, 4.3);

        List<Destination> historyTop = service.topKDestinations(
                List.of(city, museum),
                2,
                RecommendationService.DestinationRankingMode.COMPOSITE,
                "历史");

        assertThat(historyTop).extracting(Destination::getName).startsWith("古代文明展馆");
    }

    private Destination destination(String name, String sceneType, String category, String description, Double heat, Double rating) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setSceneType(sceneType);
        destination.setCategory(category);
        destination.setDescription(description);
        destination.setHeat(heat);
        destination.setRating(rating);
        return destination;
    }
}
