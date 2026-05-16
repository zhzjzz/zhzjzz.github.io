package com.travel.system.service;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.service.nav.MultiSpotRoutePlanner;
import com.travel.system.service.nav.NavigationDataService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryPlannerServiceTest {
    @Test
    void previewBuildsArrivalTimeAndTimelineFromRouteSegments() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        MultiSpotRoutePlanner routePlanner = mock(MultiSpotRoutePlanner.class);
        ItineraryPlannerService service = new ItineraryPlannerService(navigationDataService, routePlanner);

        when(navigationDataService.getGateNode("Spot A"))
                .thenReturn(new RoadNode(101L, "Spot A", 39.9, 116.3, null, null));
        when(navigationDataService.getGateNode("Spot B"))
                .thenReturn(new RoadNode(202L, "Spot B", 39.95, 116.4, null, null));

        MultiSpotNavigationResponse route = new MultiSpotNavigationResponse();
        route.setTotalDistance(1200.0);
        route.setTotalTime(1800.0);
        route.setSegments(List.of(new MultiSpotNavigationResponse.RouteSegment(
                "city",
                "Spot A",
                "Spot B",
                101L,
                202L,
                "walk",
                "drive",
                List.of(),
                new double[]{39.9, 116.3},
                new double[]{39.95, 116.4},
                1200.0,
                1800.0
        )));
        when(routePlanner.plan(any(MultiSpotNavigationRequest.class))).thenReturn(route);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        request.setDepartureTime(LocalDateTime.of(2026, 5, 15, 9, 0));
        request.setStrategy("SHORTEST_TIME");
        request.setOptimizeVisitOrder(true);
        request.setSpots(List.of(
                new ItineraryPlannerPreviewRequest.PlannerSpot(1L, 1L, "Spot A", 39.9, 116.3, "walk", true, 60),
                new ItineraryPlannerPreviewRequest.PlannerSpot(2L, 2L, "Spot B", 39.95, 116.4, "walk", true, 30)
        ));

        ItineraryPlannerPreviewResponse response = service.preview(request);

        assertThat(response.getTotalDistance()).isEqualTo(1200.0);
        assertThat(response.getTotalTime()).isEqualTo(7200.0);
        assertThat(response.getArrivalTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 11, 0));
        assertThat(response.getTimeline()).hasSize(3);
        assertThat(response.getTimeline().get(0).getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 9, 0));
        assertThat(response.getTimeline().get(0).getType()).isEqualTo("stay");
        assertThat(response.getTimeline().get(0).getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        assertThat(response.getTimeline().get(1).getType()).isEqualTo("city");
        assertThat(response.getTimeline().get(1).getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
        assertThat(response.getTimeline().get(1).getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 30));
        assertThat(response.getTimeline().get(2).getType()).isEqualTo("stay");
        assertThat(response.getTimeline().get(2).getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 30));
        assertThat(response.getTimeline().get(2).getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 11, 0));
        assertThat(response.getOrderedSpots())
                .extracting(ItineraryPlannerPreviewResponse.OrderedSpot::getSpotName)
                .containsExactly("Spot A", "Spot B");
    }

    @Test
    void previewSkipsUnroutableSelectedSpotsWithWarning() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        MultiSpotRoutePlanner routePlanner = mock(MultiSpotRoutePlanner.class);
        ItineraryPlannerService service = new ItineraryPlannerService(navigationDataService, routePlanner);

        when(navigationDataService.getGateNode("Missing Spot")).thenReturn(null);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        request.setSpots(List.of(
                new ItineraryPlannerPreviewRequest.PlannerSpot(1L, 1L, "Missing Spot", null, null, "walk", true, 120)
        ));

        ItineraryPlannerPreviewResponse response = service.preview(request);

        assertThat(response.getWarnings()).contains("No route gate found for Missing Spot");
        assertThat(response.getOrderedSpots()).isEmpty();
        assertThat(response.getSegments()).isEmpty();
    }
}
