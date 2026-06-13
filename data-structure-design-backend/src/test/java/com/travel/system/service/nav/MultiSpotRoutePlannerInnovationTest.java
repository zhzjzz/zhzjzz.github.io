package com.travel.system.service.nav;

import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.RoadEdge;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiSpotRoutePlannerInnovationTest {

    @Test
    void optimizesVisitOrderAndReportsInnovationSummary() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        CityRouteService cityRouteService = mock(CityRouteService.class);
        MultiSpotRoutePlanner planner = new MultiSpotRoutePlanner(
                navigationDataService,
                new TransportModeService(),
                cityRouteService
        );

        when(navigationDataService.buildAdjacencyList("测试景区")).thenReturn(Map.of(
                1L, List.of(edge(1, 2, 100), edge(1, 3, 10)),
                2L, List.of(edge(2, 3, 100)),
                3L, List.of(edge(3, 2, 90))
        ));
        when(navigationDataService.pathToCoordinates(anyList())).thenReturn(List.of());

        MultiSpotNavigationRequest request = new MultiSpotNavigationRequest(
                "SHORTEST_DISTANCE",
                true,
                List.of(new MultiSpotNavigationRequest.SpotVisit("测试景区", List.of(1L, 2L, 3L), "walk"))
        );
        request.setTravelerProfile("ELDERLY");

        MultiSpotNavigationResponse response = planner.plan(request);

        assertThat(response.getSegments())
                .extracting(MultiSpotNavigationResponse.RouteSegment::getToNodeId)
                .containsExactly(3L, 2L);
        assertThat(response.getTotalDistance()).isEqualTo(100.0);
        assertThat(response.getInnovationSummary()).satisfies(summary -> {
            assertThat(summary.getTravelerProfile()).isEqualTo("ELDERLY");
            assertThat(summary.getOptimizedVisitOrder()).isTrue();
            assertThat(summary.getOriginalCost()).isEqualTo(200.0);
            assertThat(summary.getOptimizedCost()).isEqualTo(100.0);
            assertThat(summary.getSavedCost()).isEqualTo(100.0);
            assertThat(summary.getExplanations()).anyMatch(text -> text.contains("少走回头路"));
        });
    }

    private RoadEdge edge(long from, long to, double length) {
        return new RoadEdge(from, to, "测试景区", length, 1.0, "walk,bike", null);
    }
}
