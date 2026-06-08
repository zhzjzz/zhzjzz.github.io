package com.travel.system.service;

import com.travel.system.mapper.DestinationMapper;
import com.travel.system.model.Destination;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationServiceTest {

    private final FakeDestinationMapper destinationMapper = new FakeDestinationMapper();
    private final DestinationService service = new DestinationService(destinationMapper, new RecommendationService());

    @Test
    void listAndTopKHideBlockedDestinationNames() {
        destinationMapper.destinations = List.of(
                destination(1L, "北京吉利学院(旧址)", 9999d, 5.0),
                destination(2L, "故宫博物院", 9000d, 4.9),
                destination(3L, "天坛公园", 8500d, 4.8)
        );

        assertThat(service.list("", 1, 10))
                .extracting(Destination::getName)
                .doesNotContain("北京吉利学院(旧址)");
        assertThat(service.topK(10, "composite"))
                .extracting(Destination::getName)
                .doesNotContain("北京吉利学院(旧址)");
    }

    private Destination destination(Long id, String name, Double heat, Double rating) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setSceneType("景区");
        destination.setCategory("景区");
        destination.setHeat(heat);
        destination.setRating(rating);
        destination.setLatitude(39.9 + id);
        destination.setLongitude(116.3 + id);
        return destination;
    }

    private static class FakeDestinationMapper implements DestinationMapper {
        private List<Destination> destinations = new ArrayList<>();

        @Override
        public List<Destination> findAll() {
            return destinations;
        }

        @Override
        public Destination findById(Long id) {
            return destinations.stream()
                    .filter(destination -> destination.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Destination> findByKeyword(String keyword) {
            return destinations.stream()
                    .filter(destination -> destination.getName().contains(keyword))
                    .toList();
        }

        @Override
        public int insert(Destination destination) {
            return 1;
        }

        @Override
        public int update(Destination destination) {
            return 1;
        }
    }
}
