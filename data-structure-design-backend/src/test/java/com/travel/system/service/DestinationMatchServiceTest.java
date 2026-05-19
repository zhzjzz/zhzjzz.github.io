package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.model.Destination;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationMatchServiceTest {
    private final FakeDestinationService destinationService = new FakeDestinationService();
    private final DestinationMatchService service = new DestinationMatchService(destinationService);

    @Test
    void matchesExactDestinationName() {
        destinationService.destinations.add(destination(1L, "The Bund"));

        DestinationMatchService.MatchResult result = service.match(place("The Bund", 0));

        assertThat(result.matched()).isTrue();
        assertThat(result.destination().getId()).isEqualTo(1L);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    void matchesNormalizedDestinationName() {
        destinationService.destinations.add(destination(2L, "Yu Garden"));

        DestinationMatchService.MatchResult result = service.match(place("yu-garden", 0));

        assertThat(result.matched()).isTrue();
        assertThat(result.destination().getName()).isEqualTo("Yu Garden");
    }

    @Test
    void rejectsAmbiguousKeywordMatch() {
        destinationService.destinations.add(destination(3L, "People Square"));
        destinationService.destinations.add(destination(4L, "People Park"));

        DestinationMatchService.MatchResult result = service.match(place("People", 0));

        assertThat(result.matched()).isFalse();
        assertThat(result.reason()).contains("Ambiguous");
    }

    @Test
    void rejectsDestinationWithoutCoordinates() {
        destinationService.destinations.add(new Destination(5L, "No Coordinates", "scenic", "park", 1.0, 1.0, "", null, null));

        DestinationMatchService.MatchResult result = service.match(place("No Coordinates", 0));

        assertThat(result.matched()).isFalse();
        assertThat(result.reason()).contains("coordinates");
    }

    private ExtractedGuidePlan.Place place(String name, int orderIndex) {
        return new ExtractedGuidePlan.Place(name, 1, orderIndex, 120, "", 0.8);
    }

    private Destination destination(Long id, String name) {
        return new Destination(id, name, "scenic", "spot", 1.0, 4.5, "", 31.0 + id, 121.0 + id);
    }

    private static class FakeDestinationService extends DestinationService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationService() {
            super(null, null);
        }

        @Override
        public List<Destination> findAll() {
            return destinations;
        }

        @Override
        public List<Destination> searchForRoute(String keyword, int limit) {
            String normalized = keyword == null ? "" : keyword.toLowerCase();
            return destinations.stream()
                    .filter(destination -> destination.getName().toLowerCase().contains(normalized)
                            || normalized.contains(destination.getName().toLowerCase()))
                    .limit(limit)
                    .toList();
        }
    }
}
