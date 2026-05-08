package com.travel.system.controller.nav;

import com.travel.system.dto.CrossSpotNavigationRequest;
import com.travel.system.dto.CrossSpotNavigationResponse;
import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
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
    private static final int EXACT_ORDER_LIMIT = 10;

    public NavigationController(NavigationDataService navigationDataService,
                                TransportModeService transportModeService,
                                CityRouteService cityRouteService) {
        this.navigationDataService = navigationDataService;
        this.transportModeService = transportModeService;
        this.cityRouteService = cityRouteService;
    }
    /**
     * 规划单个景区内部的路线。根据请求策略选择最短距离、最短时间或混合交通算法，并返回节点路径、分段步骤、总距离和总时间。
     */

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
    /**
     * 规划多个景区、多个地点的连续路线。每个景区内部先按访问点顺序规划，跨景区时补充出口段、城市交通段和入口段。
     */

    @Operation(summary = "多景区多地点路线规划")
    @PostMapping("/multi-spot")
    public MultiSpotNavigationResponse multiSpotRoute(@RequestBody MultiSpotNavigationRequest request) {
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

        List<List<Long>> orderedVisitNodes = new ArrayList<>();
        for (int i = 0; i < visits.size(); i++) {
            MultiSpotNavigationRequest.SpotVisit visit = visits.get(i);
            String spotName = visit.getSpotName();
            RoadNode gate = navigationDataService.getGateNode(spotName);
            Long entryNode = i > 0 && gate != null && !sameSpot(visits.get(i - 1).getSpotName(), spotName) ? gate.getOsmid() : null;
            Long exitNode = i < visits.size() - 1 && gate != null && !sameSpot(spotName, visits.get(i + 1).getSpotName()) ? gate.getOsmid() : null;
            orderedVisitNodes.add(orderVisitNodes(
                    resolveVisitNodes(visit),
                    navigationDataService.buildAdjacencyList(spotName),
                    request.getStrategy(),
                    normalizeMode(visit.getTransportMode()),
                    Boolean.TRUE.equals(request.getOptimizeVisitOrder()),
                    entryNode,
                    exitNode
            ));
        }

        for (int i = 0; i < visits.size(); i++) {
            MultiSpotNavigationRequest.SpotVisit current = visits.get(i);
            String currentSpot = current.getSpotName();
            String currentMode = normalizeMode(current.getTransportMode());
            Map<Long, List<RoadEdge>> currentAdj = navigationDataService.buildAdjacencyList(currentSpot);
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

            if (i < visits.size() - 1) {
                MultiSpotNavigationRequest.SpotVisit next = visits.get(i + 1);
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
                        navigationDataService.buildAdjacencyList(currentSpot),
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
                        navigationDataService.buildAdjacencyList(nextSpot),
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

        resp.setTotalDistance(totalDistance);
        resp.setTotalTime(totalTime);
        return resp;
    }
    /**
     * 返回指定景区的全部路网节点，供前端地图标点、起终点选择和调试使用。
     */

    @Operation(summary = "获取某景区全部节点")
    @GetMapping("/nodes")
    public List<RoadNode> getNodes(@RequestParam(required = false) String spotName) {
        return navigationDataService.loadNodes(spotName);
    }
    /**
     * 返回指定景区的全部路网边，供前端展示道路数据或排查路径规划问题。
     */

    @Operation(summary = "获取某景区全部边")
    @GetMapping("/edges")
    public List<RoadEdge> getEdges(@RequestParam(required = false) String spotName) {
        return navigationDataService.loadEdges(spotName);
    }

    // ======== 核心算法占位方法（用户自行实现） ========

    // adj: 路网邻接表，key=节点ID，value=从该节点出发的所有可通行边
    // start: 起点节点ID
    // end: 终点节点ID

    /**

     * 使用 Dijkstra 算法计算最短距离路径。边权重取道路长度；带交通方式参数时会过滤该方式不可通行的道路。

     */
    private NavigationResponse shortestDistancePath(Map<Long, List<RoadEdge>> adj, Long start, Long end) {
        return dijkstra(adj, start, end, edge -> edgeLength(edge), edge -> DEFAULT_MODE);
    }

    /**

     * 使用 Dijkstra 算法计算最短距离路径。边权重取道路长度；带交通方式参数时会过滤该方式不可通行的道路。

     */
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

    /**

     * 使用 Dijkstra 算法计算最短时间路径。边权重取指定交通方式下的通行时间，不可通行道路会被跳过。

     */
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

    /**

     * 使用 Dijkstra 算法计算混合交通路径。每条边会在允许的交通方式中选择耗时最短的一种作为该边的权重和步骤方式。

     */
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
    /**
     * 通用 Dijkstra 最短路实现。adj 是邻接表，start/end 是起终点节点 ID，weight 用来计算边权重，modeSelector 用来决定每条边在返回步骤中显示的交通方式。
     */

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

    /**

     * 安全读取道路长度；当数据库长度为空时返回 0，避免路径汇总出现空指针。

     */
    private double edgeLength(RoadEdge edge) {
        return edge.getLength() != null ? edge.getLength() : 0.0;
    }

    /**

     * 标准化交通方式字符串；空值默认步行，并将旧的 cart 参数兼容转换为数据库使用的 bike。

     */
    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return DEFAULT_MODE;
        }
        String normalized = mode.trim().toLowerCase();
        return "cart".equals(normalized) ? "bike" : normalized;
    }

    /**

     * 标准化混合交通方式列表，去掉空值和重复项；如果列表为空则默认只使用步行。

     */
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
    /**
     * 根据策略字符串选择具体路径算法。SHORTEST_TIME 使用时间作为边权重，其余情况默认使用距离作为边权重。
     */

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

    /**

     * 解析一次景区访问中的节点列表；如果前端没有选择节点，则使用该景区出入口节点作为兜底。

     */
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
    /**
     * 决定同一景区内多个访问点的访问顺序。不开启优化时保持前端顺序；点数不超过阈值时精确求最优，超过阈值时使用近似优化。
     */

    private List<Long> orderVisitNodes(List<Long> nodeIds,
                                       Map<Long, List<RoadEdge>> adj,
                                       String strategy,
                                       String transportMode,
                                       boolean optimizeVisitOrder,
                                       Long entryNode,
                                       Long exitNode) {
        if (!optimizeVisitOrder || nodeIds == null || nodeIds.size() <= 2) {
            return nodeIds == null ? List.of() : nodeIds;
        }
        if (nodeIds.size() <= EXACT_ORDER_LIMIT) {
            return exactBestOrder(nodeIds, adj, strategy, transportMode, entryNode, exitNode);
        }
        return approximateBestOrder(nodeIds, adj, strategy, transportMode, entryNode, exitNode);
    }
    /**
     * 使用动态规划精确求解访问点顺序。适合 10 个以内访问点，代价矩阵来自点到点最短路，并可叠加入口和出口成本。
     */

    private List<Long> exactBestOrder(List<Long> nodeIds,
                                      Map<Long, List<RoadEdge>> adj,
                                      String strategy,
                                      String transportMode,
                                      Long entryNode,
                                      Long exitNode) {
        int n = nodeIds.size();
        double[][] cost = buildCostMatrix(nodeIds, adj, strategy, transportMode);
        double[] entryCost = buildAnchorCost(entryNode, nodeIds, adj, strategy, transportMode, true);
        double[] exitCost = buildAnchorCost(exitNode, nodeIds, adj, strategy, transportMode, false);
        int states = 1 << n;
        double[][] dp = new double[states][n];
        int[][] prev = new int[states][n];
        for (double[] row : dp) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        for (int[] row : prev) {
            Arrays.fill(row, -1);
        }
        for (int start = 0; start < n; start++) {
            dp[1 << start][start] = entryCost[start];
        }

        for (int mask = 1; mask < states; mask++) {
            for (int last = 0; last < n; last++) {
                if (!Double.isFinite(dp[mask][last])) {
                    continue;
                }
                for (int next = 1; next < n; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    double nextCost = dp[mask][last] + cost[last][next];
                    int nextMask = mask | (1 << next);
                    if (nextCost < dp[nextMask][next]) {
                        dp[nextMask][next] = nextCost;
                        prev[nextMask][next] = last;
                    }
                }
            }
        }

        int fullMask = states - 1;
        int bestLast = 0;
        double best = Double.POSITIVE_INFINITY;
        for (int last = 0; last < n; last++) {
            double candidate = dp[fullMask][last] + exitCost[last];
            if (candidate < best) {
                best = candidate;
                bestLast = last;
            }
        }
        if (!Double.isFinite(best)) {
            return nodeIds;
        }

        LinkedList<Long> ordered = new LinkedList<>();
        int mask = fullMask;
        int current = bestLast;
        while (current >= 0) {
            ordered.addFirst(nodeIds.get(current));
            int previous = prev[mask][current];
            mask &= ~(1 << current);
            current = previous;
        }
        return ordered;
    }
    /**
     * 使用最近邻加 2-opt 生成近似最优访问顺序。适合访问点较多时降低计算量，避免大量 Dijkstra 组合导致响应过慢。
     */

    private List<Long> approximateBestOrder(List<Long> nodeIds,
                                            Map<Long, List<RoadEdge>> adj,
                                            String strategy,
                                            String transportMode,
                                            Long entryNode,
                                            Long exitNode) {
        double[][] cost = buildCostMatrix(nodeIds, adj, strategy, transportMode);
        double[] entryCost = buildAnchorCost(entryNode, nodeIds, adj, strategy, transportMode, true);
        double[] exitCost = buildAnchorCost(exitNode, nodeIds, adj, strategy, transportMode, false);
        List<Integer> order = nearestNeighborOrder(cost, entryCost);
        improveWithTwoOpt(order, cost, entryCost, exitCost);
        return order.stream().map(nodeIds::get).toList();
    }
    /**
     * 构建访问点之间的代价矩阵。矩阵元素表示从 nodeIds[i] 到 nodeIds[j] 的最短距离或最短时间，不可达时记为无穷大。
     */

    private double[][] buildCostMatrix(List<Long> nodeIds,
                                       Map<Long, List<RoadEdge>> adj,
                                       String strategy,
                                       String transportMode) {
        int n = nodeIds.size();
        double[][] cost = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    cost[i][j] = 0.0;
                    continue;
                }
                NavigationResponse route = planByStrategy(adj, nodeIds.get(i), nodeIds.get(j), strategy, transportMode);
                double metric = "SHORTEST_TIME".equalsIgnoreCase(strategy) ? safe(route.getTotalTime()) : safe(route.getTotalDistance());
                cost[i][j] = metric > 0 ? metric : Double.POSITIVE_INFINITY;
            }
        }
        return cost;
    }
    /**
     * 计算入口或出口锚点与每个访问点之间的代价。anchorToNode 为 true 表示入口到点，为 false 表示点到出口。
     */

    private double[] buildAnchorCost(Long anchorNode,
                                     List<Long> nodeIds,
                                     Map<Long, List<RoadEdge>> adj,
                                     String strategy,
                                     String transportMode,
                                     boolean anchorToNode) {
        double[] cost = new double[nodeIds.size()];
        if (anchorNode == null) {
            Arrays.fill(cost, 0.0);
            return cost;
        }
        for (int i = 0; i < nodeIds.size(); i++) {
            NavigationResponse route = anchorToNode
                    ? planByStrategy(adj, anchorNode, nodeIds.get(i), strategy, transportMode)
                    : planByStrategy(adj, nodeIds.get(i), anchorNode, strategy, transportMode);
            double metric = "SHORTEST_TIME".equalsIgnoreCase(strategy) ? safe(route.getTotalTime()) : safe(route.getTotalDistance());
            cost[i] = metric > 0 ? metric : Double.POSITIVE_INFINITY;
        }
        return cost;
    }

    /**

     * 使用最近邻策略生成多访问点的近似访问顺序，作为大规模路径优化的初始解。

     */
    private List<Integer> nearestNeighborOrder(double[][] cost, double[] entryCost) {
        int n = cost.length;
        List<Integer> order = new ArrayList<>();
        boolean[] used = new boolean[n];
        int current = 0;
        double bestEntry = Double.POSITIVE_INFINITY;
        for (int candidate = 0; candidate < n; candidate++) {
            if (entryCost[candidate] < bestEntry) {
                current = candidate;
                bestEntry = entryCost[candidate];
            }
        }
        order.add(current);
        used[current] = true;

        for (int step = 1; step < n; step++) {
            int best = -1;
            double bestCost = Double.POSITIVE_INFINITY;
            for (int candidate = 1; candidate < n; candidate++) {
                if (!used[candidate] && cost[current][candidate] < bestCost) {
                    best = candidate;
                    bestCost = cost[current][candidate];
                }
            }
            if (best < 0) {
                for (int candidate = 1; candidate < n; candidate++) {
                    if (!used[candidate]) {
                        best = candidate;
                        break;
                    }
                }
            }
            order.add(best);
            used[best] = true;
            current = best;
        }
        return order;
    }

    /**

     * 使用 2-opt 对访问顺序做局部优化，通过反转区间降低总路径代价。

     */
    private void improveWithTwoOpt(List<Integer> order, double[][] cost, double[] entryCost, double[] exitCost) {
        boolean improved = true;
        while (improved) {
            improved = false;
            double currentCost = orderCost(order, cost, entryCost, exitCost);
            for (int i = 0; i < order.size() - 1; i++) {
                for (int j = i + 1; j < order.size(); j++) {
                    Collections.reverse(order.subList(i, j + 1));
                    double swappedCost = orderCost(order, cost, entryCost, exitCost);
                    if (swappedCost < currentCost) {
                        currentCost = swappedCost;
                        improved = true;
                    } else {
                        Collections.reverse(order.subList(i, j + 1));
                    }
                }
            }
        }
    }

    /**

     * 计算一个访问顺序的总代价，包括入口到首点、点到点以及末点到出口的成本。

     */
    private double orderCost(List<Integer> order, double[][] cost, double[] entryCost, double[] exitCost) {
        if (order.isEmpty()) {
            return 0.0;
        }
        double total = entryCost[order.get(0)];
        for (int i = 0; i < order.size() - 1; i++) {
            total += cost[order.get(i)][order.get(i + 1)];
        }
        total += exitCost[order.get(order.size() - 1)];
        return total;
    }
    /**
     * 把景区内部 NavigationResponse 转换为多景区响应中的路线段，补充景区名、起终点、交通方式、坐标路径、距离和时间。
     */

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
    /**
     * 创建跨景区城市交通段。起终点使用两个景区出入口坐标，距离和耗时优先读取 city_routes 表，缺失时返回可供前端画线的空成本段。
     */

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
            segment.setDistance(cityRoute.getDistance());
            segment.setTime(cityRoute.getTimeCost());
        } else {
            segment.setTransitType("城市交通");
            segment.setDistance(0.0);
            segment.setTime(0.0);
        }
        return segment;
    }

    /**

     * 将空值或非有限浮点值转换为 0，避免距离和时间累加时出现异常值。

     */
    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    /**

     * 为单条道路在多个交通方式中选择耗时最短且允许通行的方式。

     */
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

    /**

     * 判断两个景区名称是否表示同一个景区，比较前会统一做空值和大小写处理。

     */
    private boolean sameSpot(String fromSpot, String toSpot) {
        return normalizeSpotName(fromSpot).equals(normalizeSpotName(toSpot));
    }

    /**

     * 标准化景区名称，用于景区名称比较，避免空格和大小写差异影响判断。

     */
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

    /**

     * Dijkstra 优先队列中的节点状态，保存节点 ID 和从起点到该节点的当前最小代价。

     */
    private record NodeState(Long nodeId, double distance) {
    }

    /**

     * 混合交通选择结果，保存某条边最终采用的交通方式及对应耗时。

     */
    private record MixedChoice(String mode, double time) {
    }
}
