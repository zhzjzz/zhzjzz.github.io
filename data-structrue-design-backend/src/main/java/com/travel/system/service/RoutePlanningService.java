package com.travel.system.service;

import com.travel.system.dto.RoutePlanResponse;
import com.travel.system.model.RoadEdge;
import com.travel.system.repository.RoadEdgeRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoutePlanningService {

    private final RoadEdgeRepository roadEdgeRepository;

    public RoutePlanningService(RoadEdgeRepository roadEdgeRepository) {
        this.roadEdgeRepository = roadEdgeRepository;
    }

    public RoutePlanResponse shortestPath(Long fromId, Long toId, String strategy, String transport) {
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[1]));

        dist.put(fromId, 0.0);
        pq.offer(new long[]{fromId, 0});

        while (!pq.isEmpty()) {
            long[] cur = pq.poll();
            long node = cur[0];
            double curDist = dist.getOrDefault(node, Double.MAX_VALUE);
            if (node == toId) {
                break;
            }

            for (RoadEdge edge : roadEdgeRepository.findByFromNodeId(node)) {
                if (!allow(edge.getAllowedTransport(), transport)) {
                    continue;
                }
                long next = edge.getToNode().getId();
                double weight = "time".equalsIgnoreCase(strategy) ? travelTime(edge) : safe(edge.getDistanceMeters());
                double cand = curDist + weight;
                if (cand < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, cand);
                    prev.put(next, node);
                    pq.offer(new long[]{next, (long) cand});
                }
            }
        }

        List<Long> path = buildPath(prev, fromId, toId);
        double totalDistance = 0;
        double totalTimeMinutes = 0;
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

        return new RoutePlanResponse(path, totalDistance, totalTimeMinutes);
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
}
