package com.travel.system.service;

import com.travel.system.dto.RoutePlanResponse;
import com.travel.system.model.RoadEdge;
import com.travel.system.mapper.RoadEdgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RoutePlanningService {

private final RoadEdgeMapper roadEdgeRepository;

public RoutePlanningService(RoadEdgeMapper roadEdgeRepository) {
        this.roadEdgeRepository = roadEdgeRepository;
    }

    public RoutePlanResponse shortestPath(Long fromId, Long toId, String strategy, String transport) {
        PathResult result = computePath(fromId, toId, strategy, transport);
        return new RoutePlanResponse(result.pathNodeIds(), List.of(toId), result.totalDistanceMeters(), result.totalTravelMinutes());
    }

    public RoutePlanResponse multiTargetPath(Long fromId, List<Long> targetNodeIds, String strategy, String transport) {
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "多目标规划至少需要一个目标节点");
        }

        LinkedHashSet<Long> remainingTargets = new LinkedHashSet<>();
        for (Long targetId : targetNodeIds) {
            if (targetId == null) {
                continue;
            }
            if (!targetId.equals(fromId)) {
                remainingTargets.add(targetId);
            }
        }

        if (remainingTargets.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "目标节点不能全部与起点相同");
        }

        List<Long> visitOrder = new ArrayList<>();
        List<Long> fullPath = new ArrayList<>();
        fullPath.add(fromId);

        long current = fromId;
        double totalDistance = 0.0;
        double totalTime = 0.0;

        while (!remainingTargets.isEmpty()) {
            long currentNode = current;
            PathChoice nextChoice = remainingTargets.stream()
                    .map(targetId -> new PathChoice(targetId, computePath(currentNode, targetId, strategy, transport)))
                    .filter(choice -> !choice.pathResult().pathNodeIds().isEmpty())
                    .min(Comparator.comparingDouble(choice -> choice.pathResult().cost(strategy)))
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "存在无法到达的目标节点，请检查道路图连通性"));

            appendSegment(fullPath, nextChoice.pathResult().pathNodeIds());
            totalDistance += nextChoice.pathResult().totalDistanceMeters();
            totalTime += nextChoice.pathResult().totalTravelMinutes();
            current = nextChoice.targetNodeId();
            visitOrder.add(nextChoice.targetNodeId());
            remainingTargets.remove(nextChoice.targetNodeId());
        }

        PathResult returnPath = computePath(current, fromId, strategy, transport);
        if (returnPath.pathNodeIds().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "无法从最后一个目标点返回起点，请检查道路图连通性");
        }
        appendSegment(fullPath, returnPath.pathNodeIds());
        totalDistance += returnPath.totalDistanceMeters();
        totalTime += returnPath.totalTravelMinutes();

        return new RoutePlanResponse(fullPath, visitOrder, totalDistance, totalTime);
    }

    public Map<Long, Double> shortestDistanceMap(Long fromId, String transport) {
        Map<Long, Double> dist = new HashMap<>();
        runShortestPath(fromId, "distance", transport, dist, new HashMap<>());
        return dist;
    }

    private PathResult computePath(Long fromId, Long toId, String strategy, String transport) {
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        runShortestPath(fromId, strategy, transport, dist, prev);

        List<Long> path = buildPath(prev, fromId, toId);
        if (path.isEmpty()) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        double totalDistance = 0.0;
        double totalTimeMinutes = 0.0;
        for (int i = 0; i + 1 < path.size(); i++) {
            Long s = path.get(i);
            Long t = path.get(i + 1);
            RoadEdge edge = roadEdgeRepository.findByFromNodeId(s).stream()
                    .filter(e -> e.getToNode().getId().equals(t))
                    .findFirst()
                    .orElse(null);
            if (edge != null) {
                totalDistance += safe(edge.getDistanceMeters());
                totalTimeMinutes += travelTime(edge);
            }
        }
        return new PathResult(path, totalDistance, totalTimeMinutes);
    }

    private void runShortestPath(Long fromId,
                                 String strategy,
                                 String transport,
                                 Map<Long, Double> dist,
                                 Map<Long, Long> prev) {
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));
        dist.put(fromId, 0.0);
        pq.offer(new NodeDistance(fromId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            if (current.distance() > dist.getOrDefault(current.nodeId(), Double.MAX_VALUE)) {
                continue;
            }

            for (RoadEdge edge : roadEdgeRepository.findByFromNodeId(current.nodeId())) {
                if (!allow(edge.getAllowedTransport(), transport)) {
                    continue;
                }
                long next = edge.getToNode().getId();
                double weight = "time".equalsIgnoreCase(strategy) ? travelTime(edge) : safe(edge.getDistanceMeters());
                double candidate = current.distance() + weight;
                if (candidate < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, candidate);
                    prev.put(next, current.nodeId());
                    pq.offer(new NodeDistance(next, candidate));
                }
            }
        }
    }

    private List<Long> buildPath(Map<Long, Long> prev, Long fromId, Long toId) {
        LinkedList<Long> path = new LinkedList<>();
        Long cur = toId;
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(fromId)) {
                return path;
            }
            cur = prev.get(cur);
        }
        return List.of();
    }

    private void appendSegment(List<Long> fullPath, List<Long> segment) {
        if (segment.isEmpty()) {
            return;
        }
        int startIndex = fullPath.isEmpty() ? 0 : 1;
        for (int i = startIndex; i < segment.size(); i++) {
            fullPath.add(segment.get(i));
        }
    }

    private boolean allow(String allowedTransport, String currentTransport) {
        if (allowedTransport == null || currentTransport == null) {
            return true;
        }
        return Arrays.stream(allowedTransport.split(","))
                .map(String::trim)
                .anyMatch(v -> v.equalsIgnoreCase(currentTransport));
    }

    private double travelTime(RoadEdge edge) {
        double speed = safe(edge.getIdealSpeed()) * Math.max(0.1, safe(edge.getCongestion()));
        return speed <= 0 ? Double.MAX_VALUE : safe(edge.getDistanceMeters()) / speed;
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    private record NodeDistance(Long nodeId, double distance) {
    }

    private record PathResult(List<Long> pathNodeIds, double totalDistanceMeters, double totalTravelMinutes) {
        private double cost(String strategy) {
            return "time".equalsIgnoreCase(strategy) ? totalTravelMinutes : totalDistanceMeters;
        }
    }

    private record PathChoice(Long targetNodeId, PathResult pathResult) {
    }
}
