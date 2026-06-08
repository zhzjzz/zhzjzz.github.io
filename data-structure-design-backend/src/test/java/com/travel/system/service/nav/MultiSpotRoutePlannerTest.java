package com.travel.system.service.nav;

import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.CityRoute;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiSpotRoutePlannerTest {

    private final FakeNavigationDataService navigationDataService = new FakeNavigationDataService();
    private final TransportModeService transportModeService = new TransportModeService();
    private final FakeCityRouteService cityRouteService = new FakeCityRouteService();
    private final MultiSpotRoutePlanner planner = new MultiSpotRoutePlanner(
            navigationDataService,
            transportModeService,
            cityRouteService
    );

    @Test
    void returnToStartAcrossDifferentSpotsAddsCityReturnAndFinalEntry() {
        navigationDataService.gates.put("SpotA", node(100L, "SpotA", 116.0, 39.0));
        navigationDataService.gates.put("SpotB", node(200L, "SpotB", 117.0, 40.0));
        navigationDataService.adjacencyBySpot.put("SpotA", Map.of(
                100L, List.of(edge(100L, 101L, "SpotA", 20.0)),
                101L, List.of(edge(101L, 102L, "SpotA", 30.0)),
                102L, List.of(edge(102L, 100L, "SpotA", 40.0))
        ));
        navigationDataService.adjacencyBySpot.put("SpotB", Map.of(
                200L, List.of(edge(200L, 201L, "SpotB", 25.0)),
                201L, List.of(edge(201L, 202L, "SpotB", 35.0)),
                202L, List.of(edge(202L, 200L, "SpotB", 45.0))
        ));
        cityRouteService.routes.put("SpotA->SpotB", new CityRoute(1L, "SpotA", "SpotB", "drive", 10.0, 5.0));
        cityRouteService.routes.put("SpotB->SpotA", new CityRoute(2L, "SpotB", "SpotA", "drive", 12.0, 6.0));

        MultiSpotNavigationRequest request = new MultiSpotNavigationRequest(
                "SHORTEST_DISTANCE",
                false,
                101L,
                true,
                List.of(
                        new MultiSpotNavigationRequest.SpotVisit("SpotA", List.of(101L, 102L), "walk"),
                        new MultiSpotNavigationRequest.SpotVisit("SpotB", List.of(201L, 202L), "walk")
                )
        );

        MultiSpotNavigationResponse response = planner.plan(request);

        assertThat(response.getSegments())
                .extracting(MultiSpotNavigationResponse.RouteSegment::getType)
                .containsSequence("spot_exit", "city", "return_to_start");
        assertThat(response.getSegments())
                .filteredOn(segment -> "city".equals(segment.getType()))
                .extracting(MultiSpotNavigationResponse.RouteSegment::getFromSpotName,
                        MultiSpotNavigationResponse.RouteSegment::getToSpotName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SpotA", "SpotB"),
                        org.assertj.core.groups.Tuple.tuple("SpotB", "SpotA")
                );
        assertThat(response.getSegments().get(response.getSegments().size() - 1).getToNodeId()).isEqualTo(101L);
    }

    @Test
    void optimizeVisitOrderReordersDifferentSpotsByCityDistance() {
        navigationDataService.gates.put("SpotA", node(100L, "SpotA", 116.0, 39.0));
        navigationDataService.gates.put("SpotB", node(200L, "SpotB", 116.1, 39.0));
        navigationDataService.gates.put("SpotC", node(300L, "SpotC", 116.2, 39.0));
        navigationDataService.adjacencyBySpot.put("SpotA", Map.of());
        navigationDataService.adjacencyBySpot.put("SpotB", Map.of());
        navigationDataService.adjacencyBySpot.put("SpotC", Map.of());
        cityRouteService.routes.put("SpotA->SpotB", new CityRoute(1L, "SpotA", "SpotB", "drive", 5.0, 5.0));
        cityRouteService.routes.put("SpotB->SpotC", new CityRoute(2L, "SpotB", "SpotC", "drive", 5.0, 5.0));
        cityRouteService.routes.put("SpotA->SpotC", new CityRoute(3L, "SpotA", "SpotC", "drive", 20.0, 20.0));
        cityRouteService.routes.put("SpotC->SpotB", new CityRoute(4L, "SpotC", "SpotB", "drive", 5.0, 5.0));

        MultiSpotNavigationRequest request = new MultiSpotNavigationRequest(
                "SHORTEST_DISTANCE",
                true,
                100L,
                false,
                List.of(
                        new MultiSpotNavigationRequest.SpotVisit("SpotA", List.of(100L), "walk"),
                        new MultiSpotNavigationRequest.SpotVisit("SpotC", List.of(300L), "walk"),
                        new MultiSpotNavigationRequest.SpotVisit("SpotB", List.of(200L), "walk")
                )
        );

        MultiSpotNavigationResponse response = planner.plan(request);

        assertThat(response.getSegments())
                .filteredOn(segment -> "city".equals(segment.getType()))
                .extracting(MultiSpotNavigationResponse.RouteSegment::getFromSpotName,
                        MultiSpotNavigationResponse.RouteSegment::getToSpotName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("SpotA", "SpotB"),
                        org.assertj.core.groups.Tuple.tuple("SpotB", "SpotC")
                );
    }

    private RoadNode node(Long id, String spotName, double lng, double lat) {
        return new RoadNode(id, spotName, lat, lng, null, null);
    }

    private RoadEdge edge(Long from, Long to, String spotName, double length) {
        return new RoadEdge(from, to, spotName, length, 1.0, "walk,bike", null);
    }

    private static class FakeNavigationDataService extends NavigationDataService {
        private final Map<String, RoadNode> gates = new HashMap<>();
        private final Map<String, Map<Long, List<RoadEdge>>> adjacencyBySpot = new HashMap<>();

        private FakeNavigationDataService() {
            super(null, null, null);
        }

        @Override
        public RoadNode getGateNode(String spotName) {
            return gates.get(spotName);
        }

        @Override
        public Map<Long, List<RoadEdge>> buildAdjacencyList(String spotName) {
            return adjacencyBySpot.getOrDefault(spotName, Map.of());
        }

        @Override
        public List<double[]> pathToCoordinates(List<Long> nodeIds) {
            List<double[]> coords = new ArrayList<>();
            for (Long ignored : nodeIds) {
                coords.add(new double[]{39.0, 116.0});
            }
            return coords;
        }
    }

    private static class FakeCityRouteService extends CityRouteService {
        private final Map<String, CityRoute> routes = new HashMap<>();

        private FakeCityRouteService() {
            super(null);
        }

        @Override
        public CityRoute findByFromAndTo(String fromSpot, String toSpot) {
            return routes.get(fromSpot + "->" + toSpot);
        }
    }
}
