package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.model.RoadNode;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.mapper.RoadNodeMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FacilitySearchService {
private final FacilityMapper facilityRepository;
private final RoadNodeMapper roadNodeRepository;
    private final RoutePlanningService routePlanningService;

public FacilitySearchService(FacilityMapper facilityRepository,
                                 RoadNodeMapper roadNodeRepository,
                                 RoutePlanningService routePlanningService) {
        this.facilityRepository = facilityRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.routePlanningService = routePlanningService;
    }

    public List<FacilityQueryResult> searchNearby(Long fromNodeId,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters,
                                                  String transport) {
        List<Facility> facilities = facilityRepository.findAll();
        List<RoadNode> roadNodes = roadNodeRepository.findAll();
        Map<Long, Double> distanceMap = routePlanningService.shortestDistanceMap(fromNodeId, transport);

        return facilities.stream()
                .filter(facility -> matchesFacilityType(facility, facilityType))
                .filter(facility -> matchesKeyword(facility, keyword))
                .map(facility -> toResult(facility, roadNodes, distanceMap))
                .filter(Objects::nonNull)
                .filter(result -> maxDistanceMeters == null || result.getRouteDistanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(FacilityQueryResult::getRouteDistanceMeters))
                .toList();
    }

    private FacilityQueryResult toResult(Facility facility,
                                         List<RoadNode> roadNodes,
                                         Map<Long, Double> distanceMap) {
        RoadNode nearestNode = findNearestNode(facility, roadNodes);
        if (nearestNode == null) {
            return null;
        }
        Double routeDistance = distanceMap.get(nearestNode.getId());
        if (routeDistance == null || routeDistance.isInfinite()) {
            return null;
        }
        return new FacilityQueryResult(facility, nearestNode.getId(), nearestNode.getName(), routeDistance);
    }

    private boolean matchesFacilityType(Facility facility, String facilityType) {
        if (facilityType == null || facilityType.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getFacilityType(), facilityType);
    }

    private boolean matchesKeyword(Facility facility, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getName(), keyword)
                || containsIgnoreCase(facility.getFacilityType(), keyword)
                || (facility.getDestination() != null && containsIgnoreCase(facility.getDestination().getName(), keyword));
    }

    private RoadNode findNearestNode(Facility facility, List<RoadNode> roadNodes) {
        List<RoadNode> candidates = roadNodes.stream()
                .filter(node -> node.getLatitude() != null && node.getLongitude() != null)
                .toList();

        Double lat = facility.getLatitude();
        Double lng = facility.getLongitude();

        if (lat == null || lng == null) {
            if (facility.getDestination() != null) {
                lat = facility.getDestination().getLatitude();
                lng = facility.getDestination().getLongitude();
            }
        }

        if (lat != null && lng != null && !candidates.isEmpty()) {
            final double targetLat = lat;
            final double targetLng = lng;
            return candidates.stream()
                    .min(Comparator.comparingDouble(node -> squaredDistance(node, targetLat, targetLng)))
                    .orElse(null);
        }

        if (!roadNodes.isEmpty()) {
            return roadNodes.get(0);
        }
        return null;
    }

    private double squaredDistance(RoadNode node, double lat, double lng) {
        double dLat = node.getLatitude() - lat;
        double dLng = node.getLongitude() - lng;
        return dLat * dLat + dLng * dLng;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
