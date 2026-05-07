package com.travel.system.controller.nav;

import com.travel.system.dto.CrossSpotNavigationRequest;
import com.travel.system.dto.CrossSpotNavigationResponse;
import com.travel.system.dto.NavigationRequest;
import com.travel.system.dto.NavigationResponse;
import com.travel.system.dto.RouteStep;
import com.travel.system.model.nav.CityRoute;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.service.nav.CityRouteService;
import com.travel.system.service.nav.NavigationDataService;
import com.travel.system.service.nav.TransportModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/nav/route")
@Tag(name = "导航路径规划", description = "路线规划与节点/边查询接口")
public class NavigationController {

    private final NavigationDataService navigationDataService;
    private final TransportModeService transportModeService;
    private final CityRouteService cityRouteService;

    private static final String DEFAULT_MODE = "walk";

    public NavigationController(NavigationDataService navigationDataService,
                                TransportModeService transportModeService,
                                CityRouteService cityRouteService) {
        this.navigationDataService = navigationDataService;
        this.transportModeService = transportModeService;
        this.cityRouteService = cityRouteService;
    }

    @Operation(summary = "单景区路线规划")
    @PostMapping("/plan")
    public NavigationResponse planRoute(@RequestBody NavigationRequest request) {
        String spotName = request.getSpotName();
        Map<Long, List<RoadEdge>> adj = navigationDataService.buildAdjacencyList(spotName);

        NavigationResponse response = new NavigationResponse();
        response.setPath(List.of());
        response.setSteps(List.of());
        response.setTotalDistance(0.0);
        response.setTotalTime(0.0);

        String strategy = request.getStrategy() != null ? request.getStrategy() : "SHORTEST_DISTANCE";
        Long start = request.getStartNodeId();
        Long end = request.getEndNodeId();

        if (start == null || end == null || adj.isEmpty()) {
            return response;
        }

        return switch (strategy.toUpperCase()) {
            case "SHORTEST_TIME" -> shortestTimePath(adj, start, end, request.getTransportMode());
            case "MIXED_TRANSPORT" -> mixedTransportPath(adj, start, end, request.getMixedTransportModes());
            default -> shortestDistancePath(adj, start, end);
        };
    }

    /**
     * 三段式跨景区导航：
     * 第一段：起始景区内部路径（fromNodeId → fromGateNode）
     * 第二段：城市交通（fromGate lng/lat → toGate lng/lat），前端用高德 Driving 插件画线
     * 第三段：目标景区内部路径（toGateNode → toNodeId）
     */
    @Operation(summary = "跨景区三段式导航")
    @PostMapping("/cross-spot")
    public CrossSpotNavigationResponse crossSpotRoute(@RequestBody CrossSpotNavigationRequest request) {
        CrossSpotNavigationResponse resp = new CrossSpotNavigationResponse();
        resp.setMicroPathStart(List.of());
        resp.setMicroPathEnd(List.of());
        resp.setTotalDistance(0.0);
        resp.setTotalTime(0.0);

        String fromSpot = request.getFromSpotName();
        String toSpot = request.getToSpotName();
        Long fromNodeId = request.getFromNodeId();
        Long toNodeId = request.getToNodeId();

        if (fromSpot == null || toSpot == null) {
            return resp;
        }

        // 获取出入口节点
        RoadNode fromGate = navigationDataService.getGateNode(fromSpot);
        RoadNode toGate = navigationDataService.getGateNode(toSpot);

        if (fromGate == null || toGate == null) {
            return resp;
        }
        if (fromNodeId == null) {
            fromNodeId = fromGate.getOsmid();
        }
        if (toNodeId == null) {
            toNodeId = toGate.getOsmid();
        }

        double totalDist = 0;
        double totalTime = 0;

        if (sameSpot(fromSpot, toSpot)) {
            Map<Long, List<RoadEdge>> adj = navigationDataService.buildAdjacencyList(fromSpot);
            NavigationResponse direct = shortestDistancePath(adj, fromNodeId, toNodeId);
            if (direct.getPath() != null && !direct.getPath().isEmpty()) {
                resp.setMicroPathStart(navigationDataService.pathToCoordinates(direct.getPath()));
                resp.setSegment1Distance(direct.getTotalDistance());
                resp.setSegment1Time(direct.getTotalTime());
                resp.setTotalDistance(direct.getTotalDistance());
                resp.setTotalTime(direct.getTotalTime());
            }
            return resp;
        }

        // --- 第一段：园区内从起点到出口 ---
        Map<Long, List<RoadEdge>> adjStart = navigationDataService.buildAdjacencyList(fromSpot);
        NavigationResponse seg1 = shortestDistancePath(adjStart, fromNodeId, fromGate.getOsmid());
        if (seg1.getPath() != null && !seg1.getPath().isEmpty()) {
            resp.setMicroPathStart(navigationDataService.pathToCoordinates(seg1.getPath()));
            resp.setSegment1Distance(seg1.getTotalDistance());
            resp.setSegment1Time(seg1.getTotalTime());
            totalDist += seg1.getTotalDistance() != null ? seg1.getTotalDistance() : 0;
            totalTime += seg1.getTotalTime() != null ? seg1.getTotalTime() : 0;
        }

        // --- 第二段：城市交通（查 city_routes 表）---
        resp.setCityTransitStart(new double[]{fromGate.getY(), fromGate.getX()});
        resp.setCityTransitEnd(new double[]{toGate.getY(), toGate.getX()});

        CityRoute cityRoute = cityRouteService.findByFromAndTo(fromSpot, toSpot);
        if (cityRoute == null) {
            cityRoute = cityRouteService.findByFromAndTo(toSpot, fromSpot);
        }
        if (cityRoute != null) {
            resp.setTransitType(cityRoute.getTransitType());
            resp.setSegment2Distance(cityRoute.getDistance());
            resp.setSegment2Time(cityRoute.getTimeCost());
            totalDist += cityRoute.getDistance() != null ? cityRoute.getDistance() : 0;
            totalTime += cityRoute.getTimeCost() != null ? cityRoute.getTimeCost() : 0;
        }

        // --- 第三段：园区内从入口到终点 ---
        Map<Long, List<RoadEdge>> adjEnd = navigationDataService.buildAdjacencyList(toSpot);
        NavigationResponse seg3 = shortestDistancePath(adjEnd, toGate.getOsmid(), toNodeId);
        if (seg3.getPath() != null && !seg3.getPath().isEmpty()) {
            resp.setMicroPathEnd(navigationDataService.pathToCoordinates(seg3.getPath()));
            resp.setSegment3Distance(seg3.getTotalDistance());
            resp.setSegment3Time(seg3.getTotalTime());
            totalDist += seg3.getTotalDistance() != null ? seg3.getTotalDistance() : 0;
            totalTime += seg3.getTotalTime() != null ? seg3.getTotalTime() : 0;
        }

        resp.setTotalDistance(totalDist);
        resp.setTotalTime(totalTime);
        return resp;
    }

    @Operation(summary = "获取某景区全部节点")
    @GetMapping("/nodes")
    public List<RoadNode> getNodes(@RequestParam(required = false) String spotName) {
        return navigationDataService.loadNodes(spotName);
    }

    @Operation(summary = "获取某景区全部边")
    @GetMapping("/edges")
    public List<RoadEdge> getEdges(@RequestParam(required = false) String spotName) {
        return navigationDataService.loadEdges(spotName);
    }

    // ======== 核心算法占位方法（用户自行实现） ========

    // adj: 路网邻接表，key=节点ID，value=从该节点出发的所有可通行边
    // start: 起点节点ID
    // end: 终点节点ID

    private NavigationResponse shortestDistancePath(Map<Long, List<RoadEdge>> adj, Long start, Long end) {
        return dijkstra(adj, start, end, edge -> edgeLength(edge), edge -> DEFAULT_MODE);
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

    private NavigationResponse mixedTransportPath(Map<Long, List<RoadEdge>> adj, Long start, Long end, List<String> modes) {
        List<String> transportModes = normalizeModes(modes);
        return dijkstra(
                adj,
                start,
                end,
                edge -> bestMixedMode(edge, transportModes).time,
                edge -> bestMixedMode(edge, transportModes).mode
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
        PriorityQueue<NodeState> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeState::distance));

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
                double nextDistance = current.distance() + edgeWeight;
                if (nextDistance < distance.getOrDefault(edge.getV(), Double.POSITIVE_INFINITY)) {
                    distance.put(edge.getV(), nextDistance);
                    previousEdge.put(edge.getV(), edge);
                    queue.add(new NodeState(edge.getV(), nextDistance));
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
        Collections.reverse(routeEdges);

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

    private double edgeLength(RoadEdge edge) {
        return edge.getLength() != null ? edge.getLength() : 0.0;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? DEFAULT_MODE : mode.trim().toLowerCase();
    }

    private List<String> normalizeModes(List<String> modes) {
        if (modes == null || modes.isEmpty()) {
            return List.of(DEFAULT_MODE);
        }
        List<String> normalized = modes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(mode -> !mode.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of(DEFAULT_MODE) : normalized;
    }

    private MixedChoice bestMixedMode(RoadEdge edge, List<String> modes) {
        String bestMode = DEFAULT_MODE;
        double bestTime = Double.POSITIVE_INFINITY;
        for (String mode : modes) {
            if (!transportModeService.isVehicleAllowed(edge, mode)) {
                continue;
            }
            double time = transportModeService.calculateTravelTime(edge, mode);
            if (time < bestTime) {
                bestTime = time;
                bestMode = mode;
            }
        }
        return new MixedChoice(bestMode, bestTime);
    }

    private boolean sameSpot(String fromSpot, String toSpot) {
        return normalizeSpotName(fromSpot).equals(normalizeSpotName(toSpot));
    }

    private String normalizeSpotName(String spotName) {
        return spotName == null ? "" : spotName.trim().toLowerCase();
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

    private record MixedChoice(String mode, double time) {
    }
}
