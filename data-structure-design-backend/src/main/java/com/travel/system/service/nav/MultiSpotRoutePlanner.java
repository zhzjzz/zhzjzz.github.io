package com.travel.system.service.nav;

import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.dto.NavigationResponse;
import com.travel.system.dto.RouteStep;
import com.travel.system.model.nav.CityRoute;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

@Service
public class MultiSpotRoutePlanner {
    private final NavigationDataService navigationDataService;
    private final TransportModeService transportModeService;
    private final CityRouteService cityRouteService;

    private static final String DEFAULT_MODE = "walk";
    private static final double KILOMETERS_TO_METERS = 1000.0;
    private static final double MINUTES_TO_SECONDS = 60.0;

    public MultiSpotRoutePlanner(NavigationDataService navigationDataService,
                                 TransportModeService transportModeService,
                                 CityRouteService cityRouteService) {
        this.navigationDataService = navigationDataService;
        this.transportModeService = transportModeService;
        this.cityRouteService = cityRouteService;
    }

    public MultiSpotNavigationResponse plan(MultiSpotNavigationRequest request) {
        MultiSpotNavigationResponse resp = new MultiSpotNavigationResponse();
        resp.setSegments(new ArrayList<>());
        resp.setTotalDistance(0.0);
        resp.setTotalTime(0.0);

        if (request == null || request.getVisits() == null || request.getVisits().isEmpty()) {
            return resp;
        }

        List<MultiSpotNavigationRequest.SpotVisit> visits = request.getVisits().stream()
                .filter(visit -> visit != null && visit.getSpotName() != null && !visit.getSpotName().isBlank())
                .toList();
        if (visits.isEmpty()) {
            return resp;
        }

        double totalDistance = 0.0;
        double totalTime = 0.0;

        List<VisitPlan> visitPlans = new ArrayList<>();
        for (MultiSpotNavigationRequest.SpotVisit visit : visits) {
            List<Long> nodes = resolveVisitNodes(visit);
            Map<Long, List<RoadEdge>> adj = navigationDataService.buildAdjacencyList(visit.getSpotName());
            visitPlans.add(new VisitPlan(visit, nodes, adj));
        }
        visitPlans = optimizeSpotOrderIfRequested(visitPlans, request.getStrategy(), request.getOptimizeVisitOrder());

        List<List<Long>> orderedVisitNodes = new ArrayList<>();
        for (int i = 0; i < visitPlans.size(); i++) {
            VisitPlan visitPlan = visitPlans.get(i);
            Long startNodeId = i == 0 ? request.getStartNodeId() : null;
            orderedVisitNodes.add(optimizeNodeOrderIfRequested(
                    visitPlan.nodes(),
                    visitPlan.adjacency(),
                    request.getStrategy(),
                    normalizeMode(visitPlan.visit().getTransportMode()),
                    request.getOptimizeVisitOrder(),
                    startNodeId
            ));
        }

        for (int i = 0; i < visitPlans.size(); i++) {
            MultiSpotNavigationRequest.SpotVisit current = visitPlans.get(i).visit();
            String currentSpot = current.getSpotName();
            String currentMode = normalizeMode(current.getTransportMode());
            Map<Long, List<RoadEdge>> currentAdj = visitPlans.get(i).adjacency();
            List<Long> currentNodes = orderedVisitNodes.get(i);

            if (currentNodes.size() >= 2) {
                for (int j = 0; j < currentNodes.size() - 1; j++) {
                    NavigationResponse inner = planByStrategy(
                            currentAdj,
                            currentNodes.get(j),
                            currentNodes.get(j + 1),
                            request.getStrategy(),
                            currentMode
                    );
                    MultiSpotNavigationResponse.RouteSegment segment = createInnerSegment(
                            currentSpot,
                            currentNodes.get(j),
                            currentNodes.get(j + 1),
                            currentMode,
                            inner
                    );
                    resp.getSegments().add(segment);
                    totalDistance += safe(segment.getDistance());
                    totalTime += safe(segment.getTime());
                }
            }

            if (i < visitPlans.size() - 1) {
                MultiSpotNavigationRequest.SpotVisit next = visitPlans.get(i + 1).visit();
                String nextSpot = next.getSpotName();
                if (sameSpot(currentSpot, nextSpot)) {
                    continue;
                }

                RoadNode fromGate = navigationDataService.getGateNode(currentSpot);
                RoadNode toGate = navigationDataService.getGateNode(nextSpot);
                if (fromGate == null || toGate == null) {
                    continue;
                }

                Long fromNode = currentNodes.isEmpty() ? fromGate.getOsmid() : currentNodes.get(currentNodes.size() - 1);
                List<Long> nextNodes = orderedVisitNodes.get(i + 1);
                Long toNode = nextNodes.isEmpty() ? toGate.getOsmid() : nextNodes.get(0);

                NavigationResponse exit = planByStrategy(
                        currentAdj,
                        fromNode,
                        fromGate.getOsmid(),
                        request.getStrategy(),
                        currentMode
                );
                MultiSpotNavigationResponse.RouteSegment exitSegment = createInnerSegment(
                        currentSpot,
                        fromNode,
                        fromGate.getOsmid(),
                        currentMode,
                        exit
                );
                exitSegment.setType("spot_exit");
                resp.getSegments().add(exitSegment);
                totalDistance += safe(exitSegment.getDistance());
                totalTime += safe(exitSegment.getTime());

                MultiSpotNavigationResponse.RouteSegment citySegment = createCitySegment(currentSpot, nextSpot, fromGate, toGate);
                resp.getSegments().add(citySegment);
                totalDistance += safe(citySegment.getDistance());
                totalTime += safe(citySegment.getTime());

                String nextMode = normalizeMode(next.getTransportMode());
                NavigationResponse enter = planByStrategy(
                        visitPlans.get(i + 1).adjacency(),
                        toGate.getOsmid(),
                        toNode,
                        request.getStrategy(),
                        nextMode
                );
                MultiSpotNavigationResponse.RouteSegment enterSegment = createInnerSegment(
                        nextSpot,
                        toGate.getOsmid(),
                        toNode,
                        nextMode,
                        enter
                );
                enterSegment.setType("spot_enter");
                resp.getSegments().add(enterSegment);
                totalDistance += safe(enterSegment.getDistance());
                totalTime += safe(enterSegment.getTime());
            }
        }

        if (Boolean.TRUE.equals(request.getReturnToStart()) && request.getStartNodeId() != null && !visitPlans.isEmpty()) {
            MultiSpotNavigationRequest.SpotVisit firstVisit = visitPlans.get(0).visit();
            int lastIndex = visitPlans.size() - 1;
            MultiSpotNavigationRequest.SpotVisit lastVisit = visitPlans.get(lastIndex).visit();
            List<Long> lastNodes = orderedVisitNodes.get(lastIndex);
            if (!lastNodes.isEmpty()) {
                String lastSpot = lastVisit.getSpotName();
                String firstSpot = firstVisit.getSpotName();
                String lastMode = normalizeMode(lastVisit.getTransportMode());
                Long lastNode = lastNodes.get(lastNodes.size() - 1);
                if (sameSpot(lastSpot, firstSpot)) {
                    NavigationResponse back = planByStrategy(
                            visitPlans.get(lastIndex).adjacency(),
                            lastNode,
                            request.getStartNodeId(),
                            request.getStrategy(),
                            lastMode
                    );
                    MultiSpotNavigationResponse.RouteSegment backSegment = createInnerSegment(
                            lastSpot,
                            lastNode,
                            request.getStartNodeId(),
                            lastMode,
                            back
                    );
                    backSegment.setType("return_to_start");
                    resp.getSegments().add(backSegment);
                    totalDistance += safe(backSegment.getDistance());
                    totalTime += safe(backSegment.getTime());
                } else {
                    RoadNode lastGate = navigationDataService.getGateNode(lastSpot);
                    RoadNode firstGate = navigationDataService.getGateNode(firstSpot);
                    if (lastGate != null && firstGate != null) {
                        NavigationResponse exit = planByStrategy(
                                visitPlans.get(lastIndex).adjacency(),
                                lastNode,
                                lastGate.getOsmid(),
                                request.getStrategy(),
                                lastMode
                        );
                        MultiSpotNavigationResponse.RouteSegment exitSegment = createInnerSegment(
                                lastSpot,
                                lastNode,
                                lastGate.getOsmid(),
                                lastMode,
                                exit
                        );
                        exitSegment.setType("spot_exit");
                        resp.getSegments().add(exitSegment);
                        totalDistance += safe(exitSegment.getDistance());
                        totalTime += safe(exitSegment.getTime());

                        MultiSpotNavigationResponse.RouteSegment citySegment = createCitySegment(lastSpot, firstSpot, lastGate, firstGate);
                        resp.getSegments().add(citySegment);
                        totalDistance += safe(citySegment.getDistance());
                        totalTime += safe(citySegment.getTime());

                        String firstMode = normalizeMode(firstVisit.getTransportMode());
                        NavigationResponse enter = planByStrategy(
                                visitPlans.get(0).adjacency(),
                                firstGate.getOsmid(),
                                request.getStartNodeId(),
                                request.getStrategy(),
                                firstMode
                        );
                        MultiSpotNavigationResponse.RouteSegment enterSegment = createInnerSegment(
                                firstSpot,
                                firstGate.getOsmid(),
                                request.getStartNodeId(),
                                firstMode,
                                enter
                        );
                        enterSegment.setType("return_to_start");
                        resp.getSegments().add(enterSegment);
                        totalDistance += safe(enterSegment.getDistance());
                        totalTime += safe(enterSegment.getTime());
                    }
                }
            }
        }

        resp.setTotalDistance(totalDistance);
        resp.setTotalTime(totalTime);
        return resp;
    }

    private List<VisitPlan> optimizeSpotOrderIfRequested(List<VisitPlan> visitPlans,
                                                         String strategy,
                                                         Boolean optimizeVisitOrder) {
        if (!Boolean.TRUE.equals(optimizeVisitOrder) || visitPlans.size() <= 2) {
            return visitPlans;
        }
        List<VisitPlan> remaining = new ArrayList<>(visitPlans);
        List<VisitPlan> ordered = new ArrayList<>();
        VisitPlan current = remaining.remove(0);
        ordered.add(current);
        while (!remaining.isEmpty()) {
            VisitPlan next = nearestVisitPlan(current, remaining, strategy);
            ordered.add(next);
            remaining.remove(next);
            current = next;
        }
        return ordered;
    }

    private VisitPlan nearestVisitPlan(VisitPlan current, List<VisitPlan> remaining, String strategy) {
        VisitPlan nearest = remaining.get(0);
        double bestCost = Double.POSITIVE_INFINITY;
        for (VisitPlan candidate : remaining) {
            double cost = transitionCost(current, candidate, strategy);
            if (cost < bestCost) {
                bestCost = cost;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private double transitionCost(VisitPlan current, VisitPlan candidate, String strategy) {
        if (sameSpot(current.spotName(), candidate.spotName())) {
            Long fromNode = current.lastNode();
            Long toNode = candidate.firstNode();
            if (fromNode == null || toNode == null) {
                return 0.0;
            }
            NavigationResponse route = planByStrategy(
                    current.adjacency(),
                    fromNode,
                    toNode,
                    strategy,
                    normalizeMode(current.visit().getTransportMode())
            );
            if (route.getPath() == null || route.getPath().isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            return strategyCost(route.getTotalDistance(), route.getTotalTime(), strategy);
        }

        CityRoute cityRoute = cityRouteService.findByFromAndTo(current.spotName(), candidate.spotName());
        if (cityRoute == null) {
            cityRoute = cityRouteService.findByFromAndTo(candidate.spotName(), current.spotName());
        }
        if (cityRoute != null) {
            return isShortestTime(strategy)
                    ? cityRouteTimeSeconds(cityRoute)
                    : cityRouteDistanceMeters(cityRoute);
        }
        RoadNode fromGate = navigationDataService.getGateNode(current.spotName());
        RoadNode toGate = navigationDataService.getGateNode(candidate.spotName());
        return gateDistanceMeters(fromGate, toGate);
    }

    private double strategyCost(Double distance, Double time, String strategy) {
        return isShortestTime(strategy) ? safe(time) : safe(distance);
    }

    private boolean isShortestTime(String strategy) {
        return "SHORTEST_TIME".equalsIgnoreCase(strategy == null ? "" : strategy.trim());
    }

    private double gateDistanceMeters(RoadNode fromGate, RoadNode toGate) {
        if (fromGate == null || toGate == null
                || fromGate.getX() == null || fromGate.getY() == null
                || toGate.getX() == null || toGate.getY() == null) {
            return Double.POSITIVE_INFINITY;
        }
        double lat1 = Math.toRadians(fromGate.getY());
        double lat2 = Math.toRadians(toGate.getY());
        double deltaLat = Math.toRadians(toGate.getY() - fromGate.getY());
        double deltaLng = Math.toRadians(toGate.getX() - fromGate.getX());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000.0 * c;
    }

    private List<Long> resolveVisitNodes(MultiSpotNavigationRequest.SpotVisit visit) {
        if (visit == null || visit.getNodeIds() == null) {
            RoadNode gate = navigationDataService.getGateNode(visit != null ? visit.getSpotName() : null);
            return gate == null ? List.of() : List.of(gate.getOsmid());
        }
        List<Long> nodes = visit.getNodeIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!nodes.isEmpty()) {
            return nodes;
        }
        RoadNode gate = navigationDataService.getGateNode(visit.getSpotName());
        return gate == null ? List.of() : List.of(gate.getOsmid());
    }

    private NavigationResponse planByStrategy(Map<Long, List<RoadEdge>> adj,
                                              Long start,
                                              Long end,
                                              String strategy,
                                              String transportMode) {
        String normalizedStrategy = strategy == null ? "SHORTEST_DISTANCE" : strategy.trim().toUpperCase();
        return switch (normalizedStrategy) {
            case "SHORTEST_TIME" -> shortestTimePath(adj, start, end, transportMode);
            default -> shortestDistancePath(adj, start, end, transportMode);
        };
    }

    private List<Long> optimizeNodeOrderIfRequested(List<Long> nodes,
                                                    Map<Long, List<RoadEdge>> adj,
                                                    String strategy,
                                                    String transportMode,
                                                    Boolean optimizeVisitOrder,
                                                    Long startNodeId) {
        if (!Boolean.TRUE.equals(optimizeVisitOrder) || nodes.size() <= 1) {
            if (startNodeId != null && !nodes.contains(startNodeId)) {
                List<Long> withStart = new ArrayList<>();
                withStart.add(startNodeId);
                withStart.addAll(nodes);
                return withStart;
            }
            return nodes;
        }
        List<Long> remaining = new ArrayList<>(nodes);
        List<Long> ordered = new ArrayList<>();
        Long current = startNodeId != null ? startNodeId : remaining.remove(0);
        ordered.add(current);
        remaining.remove(current);
        while (!remaining.isEmpty()) {
            Long next = nearestNode(current, remaining, adj, strategy, transportMode);
            ordered.add(next);
            remaining.remove(next);
            current = next;
        }
        return ordered;
    }

    private Long nearestNode(Long current,
                             List<Long> remaining,
                             Map<Long, List<RoadEdge>> adj,
                             String strategy,
                             String transportMode) {
        Long nearest = remaining.get(0);
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Long candidate : remaining) {
            NavigationResponse route = planByStrategy(adj, current, candidate, strategy, transportMode);
            double distance = route.getPath() == null || route.getPath().isEmpty()
                    ? Double.POSITIVE_INFINITY
                    : safe(route.getTotalDistance());
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private NavigationResponse shortestDistancePath(Map<Long, List<RoadEdge>> adj, Long start, Long end, String mode) {
        String transportMode = normalizeMode(mode);
        return dijkstra(
                adj,
                start,
                end,
                edge -> transportModeService.isVehicleAllowed(edge, transportMode)
                        ? edgeLength(edge)
                        : Double.POSITIVE_INFINITY,
                edge -> transportMode
        );
    }

    private NavigationResponse shortestTimePath(Map<Long, List<RoadEdge>> adj, Long start, Long end, String mode) {
        String transportMode = normalizeMode(mode);
        return dijkstra(
                adj,
                start,
                end,
                edge -> transportModeService.isVehicleAllowed(edge, transportMode)
                        ? transportModeService.calculateTravelTime(edge, transportMode)
                        : Double.POSITIVE_INFINITY,
                edge -> transportMode
        );
    }

    private NavigationResponse dijkstra(Map<Long, List<RoadEdge>> adj,
                                        Long start,
                                        Long end,
                                        EdgeWeight weight,
                                        EdgeMode modeSelector) {
        NavigationResponse resp = new NavigationResponse();
        resp.setPath(new ArrayList<>());
        resp.setSteps(new ArrayList<>());
        resp.setTotalDistance(0.0);
        resp.setTotalTime(0.0);

        if (start == null || end == null || adj == null || adj.isEmpty()) {
            return resp;
        }
        if (Objects.equals(start, end)) {
            resp.setPath(List.of(start));
            return resp;
        }

        Map<Long, Double> distance = new HashMap<>();
        Map<Long, RoadEdge> previousEdge = new HashMap<>();
        PriorityQueue<NodeState> queue = new PriorityQueue<>((a, b) -> Double.compare(a.distance(), b.distance()));
        distance.put(start, 0.0);
        queue.add(new NodeState(start, 0.0));

        while (!queue.isEmpty()) {
            NodeState current = queue.poll();
            if (current.distance() > distance.getOrDefault(current.nodeId(), Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (Objects.equals(current.nodeId(), end)) {
                break;
            }
            for (RoadEdge edge : adj.getOrDefault(current.nodeId(), List.of())) {
                if (edge == null || edge.getV() == null) {
                    continue;
                }
                double edgeWeight = weight.value(edge);
                if (!Double.isFinite(edgeWeight) || edgeWeight < 0) {
                    continue;
                }
                Long next = edge.getV();
                double candidate = current.distance() + edgeWeight;
                if (candidate < distance.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    distance.put(next, candidate);
                    previousEdge.put(next, edge);
                    queue.add(new NodeState(next, candidate));
                }
            }
        }

        if (!previousEdge.containsKey(end)) {
            return resp;
        }

        List<RoadEdge> routeEdges = new ArrayList<>();
        Long node = end;
        while (!Objects.equals(node, start)) {
            RoadEdge edge = previousEdge.get(node);
            if (edge == null) {
                return resp;
            }
            routeEdges.add(edge);
            node = edge.getU();
        }
        java.util.Collections.reverse(routeEdges);

        List<Long> path = new ArrayList<>();
        path.add(start);
        List<RouteStep> steps = new ArrayList<>();
        double totalDistance = 0.0;
        double totalTime = 0.0;

        for (RoadEdge edge : routeEdges) {
            String mode = modeSelector.mode(edge);
            double length = edgeLength(edge);
            double time = transportModeService.calculateTravelTime(edge, mode);
            double congestion = edge.getCongestionBase() != null ? edge.getCongestionBase() : 1.0;

            path.add(edge.getV());
            steps.add(new RouteStep(edge.getU(), edge.getV(), mode, length, time, congestion));
            totalDistance += length;
            totalTime += time;
        }

        resp.setPath(path);
        resp.setSteps(steps);
        resp.setTotalDistance(totalDistance);
        resp.setTotalTime(totalTime);
        return resp;
    }

    private MultiSpotNavigationResponse.RouteSegment createInnerSegment(String spotName,
                                                                        Long fromNodeId,
                                                                        Long toNodeId,
                                                                        String transportMode,
                                                                        NavigationResponse route) {
        MultiSpotNavigationResponse.RouteSegment segment = new MultiSpotNavigationResponse.RouteSegment();
        segment.setType("spot_inner");
        segment.setFromSpotName(spotName);
        segment.setToSpotName(spotName);
        segment.setFromNodeId(fromNodeId);
        segment.setToNodeId(toNodeId);
        segment.setTransportMode(transportMode);
        segment.setPath(route.getPath() == null ? List.of() : navigationDataService.pathToCoordinates(route.getPath()));
        segment.setDistance(route.getTotalDistance());
        segment.setTime(route.getTotalTime());
        return segment;
    }

    private MultiSpotNavigationResponse.RouteSegment createCitySegment(String fromSpot,
                                                                       String toSpot,
                                                                       RoadNode fromGate,
                                                                       RoadNode toGate) {
        MultiSpotNavigationResponse.RouteSegment segment = new MultiSpotNavigationResponse.RouteSegment();
        segment.setType("city");
        segment.setFromSpotName(fromSpot);
        segment.setToSpotName(toSpot);
        segment.setFromNodeId(fromGate.getOsmid());
        segment.setToNodeId(toGate.getOsmid());
        segment.setCityTransitStart(new double[]{fromGate.getY(), fromGate.getX()});
        segment.setCityTransitEnd(new double[]{toGate.getY(), toGate.getX()});

        CityRoute cityRoute = cityRouteService.findByFromAndTo(fromSpot, toSpot);
        if (cityRoute == null) {
            cityRoute = cityRouteService.findByFromAndTo(toSpot, fromSpot);
        }
        if (cityRoute != null) {
            segment.setTransitType(cityRoute.getTransitType());
            segment.setDistance(cityRouteDistanceMeters(cityRoute));
            segment.setTime(cityRouteTimeSeconds(cityRoute));
        } else {
            segment.setTransitType("city");
            segment.setDistance(0.0);
            segment.setTime(0.0);
        }
        return segment;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return DEFAULT_MODE;
        }
        String normalized = mode.trim().toLowerCase();
        return "cart".equals(normalized) ? "bike" : normalized;
    }

    private double edgeLength(RoadEdge edge) {
        return safe(edge == null ? null : edge.getLength());
    }

    private double cityRouteDistanceMeters(CityRoute cityRoute) {
        return safe(cityRoute == null ? null : cityRoute.getDistance()) * KILOMETERS_TO_METERS;
    }

    private double cityRouteTimeSeconds(CityRoute cityRoute) {
        return safe(cityRoute == null ? null : cityRoute.getTimeCost()) * MINUTES_TO_SECONDS;
    }

    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    private boolean sameSpot(String fromSpot, String toSpot) {
        return normalizeSpotName(fromSpot).equals(normalizeSpotName(toSpot));
    }

    private String normalizeSpotName(String spotName) {
        return spotName == null ? "" : spotName.trim().toLowerCase();
    }

    private record VisitPlan(MultiSpotNavigationRequest.SpotVisit visit,
                             List<Long> nodes,
                             Map<Long, List<RoadEdge>> adjacency) {
        private String spotName() {
            return visit == null ? "" : visit.getSpotName();
        }

        private Long firstNode() {
            return nodes == null || nodes.isEmpty() ? null : nodes.get(0);
        }

        private Long lastNode() {
            return nodes == null || nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
        }
    }

    @FunctionalInterface
    private interface EdgeWeight {
        double value(RoadEdge edge);
    }

    @FunctionalInterface
    private interface EdgeMode {
        String mode(RoadEdge edge);
    }

    private record NodeState(Long nodeId, double distance) {
    }
}
