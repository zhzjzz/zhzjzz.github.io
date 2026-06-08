package com.travel.system.controller.nav;

import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.CityRoute;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.service.nav.CityRouteService;
import com.travel.system.service.nav.MultiSpotRoutePlanner;
import com.travel.system.service.nav.NavigationDataService;
import com.travel.system.service.nav.TransportModeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NavigationControllerTest {

    @Test
    void multiSpotRouteConvertsCityRouteKilometersAndMinutesToMetersAndSeconds() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        CityRouteService cityRouteService = mock(CityRouteService.class);
        TransportModeService transportModeService = new TransportModeService();
        MultiSpotRoutePlanner routePlanner = new MultiSpotRoutePlanner(
                navigationDataService,
                transportModeService,
                cityRouteService
        );
        NavigationController controller = new NavigationController(
                navigationDataService,
                transportModeService,
                cityRouteService,
                routePlanner
        );

        RoadNode fromGate = new RoadNode(1L, "景区A", 39.9, 116.3, null, null);
        RoadNode toGate = new RoadNode(2L, "景区B", 39.95, 116.4, null, null);
        when(navigationDataService.getGateNode("景区A")).thenReturn(fromGate);
        when(navigationDataService.getGateNode("景区B")).thenReturn(toGate);
        when(navigationDataService.buildAdjacencyList("景区A")).thenReturn(Map.of());
        when(navigationDataService.buildAdjacencyList("景区B")).thenReturn(Map.of());
        when(navigationDataService.pathToCoordinates(List.of())).thenReturn(List.of());

        CityRoute cityRoute = new CityRoute(1L, "景区A", "景区B", "驾车", 16.7, 8.33);
        when(cityRouteService.findByFromAndTo("景区A", "景区B")).thenReturn(cityRoute);

        MultiSpotNavigationRequest request = new MultiSpotNavigationRequest(
                "SHORTEST_TIME",
                false,
                List.of(
                        new MultiSpotNavigationRequest.SpotVisit("景区A", List.of(1L), "walk"),
                        new MultiSpotNavigationRequest.SpotVisit("景区B", List.of(2L), "walk")
                )
        );

        MultiSpotNavigationResponse response = controller.multiSpotRoute(request);

        assertThat(response.getTotalDistance()).isEqualTo(8330.0);
        assertThat(response.getTotalTime()).isEqualTo(1002.0);
        assertThat(response.getSegments())
                .filteredOn(segment -> "city".equals(segment.getType()))
                .singleElement()
                .satisfies(segment -> {
                    assertThat(segment.getDistance()).isEqualTo(8330.0);
                    assertThat(segment.getTime()).isEqualTo(1002.0);
                });
    }

    @Test
    void multiSpotRouteOptimizesVisitOrderAndReturnsToStart() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        CityRouteService cityRouteService = mock(CityRouteService.class);
        TransportModeService transportModeService = new TransportModeService();
        MultiSpotRoutePlanner routePlanner = new MultiSpotRoutePlanner(
                navigationDataService,
                transportModeService,
                cityRouteService
        );
        NavigationController controller = new NavigationController(
                navigationDataService,
                transportModeService,
                cityRouteService,
                routePlanner
        );
        when(navigationDataService.buildAdjacencyList("Campus")).thenReturn(Map.of(
                1L, List.of(edge(1L, 2L, 10.0), edge(1L, 3L, 80.0), edge(1L, 4L, 30.0)),
                2L, List.of(edge(2L, 1L, 10.0), edge(2L, 3L, 20.0), edge(2L, 4L, 50.0)),
                3L, List.of(edge(3L, 1L, 80.0), edge(3L, 2L, 20.0), edge(3L, 4L, 10.0)),
                4L, List.of(edge(4L, 1L, 30.0), edge(4L, 2L, 50.0), edge(4L, 3L, 10.0))
        ));
        when(navigationDataService.pathToCoordinates(List.of(1L, 2L))).thenReturn(List.of());
        when(navigationDataService.pathToCoordinates(List.of(2L, 3L))).thenReturn(List.of());
        when(navigationDataService.pathToCoordinates(List.of(3L, 4L))).thenReturn(List.of());
        when(navigationDataService.pathToCoordinates(List.of(4L, 1L))).thenReturn(List.of());

        MultiSpotNavigationRequest request = new MultiSpotNavigationRequest(
                "SHORTEST_DISTANCE",
                true,
                1L,
                true,
                List.of(new MultiSpotNavigationRequest.SpotVisit("Campus", List.of(3L, 2L, 4L), "walk"))
        );

        MultiSpotNavigationResponse response = controller.multiSpotRoute(request);

        assertThat(response.getSegments()).extracting(MultiSpotNavigationResponse.RouteSegment::getToNodeId)
                .containsSubsequence(2L, 3L, 4L, 1L);
        assertThat(response.getSegments().get(response.getSegments().size() - 1).getType()).isEqualTo("return_to_start");
    }

    private RoadEdge edge(Long from, Long to, Double length) {
        RoadEdge edge = new RoadEdge();
        edge.setU(from);
        edge.setV(to);
        edge.setLength(length);
        edge.setAllowedVehicles("walk,bike,cart");
        return edge;
    }
}
