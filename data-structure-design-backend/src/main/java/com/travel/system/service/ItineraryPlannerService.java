package com.travel.system.service;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.service.nav.MultiSpotRoutePlanner;
import com.travel.system.service.nav.NavigationDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ItineraryPlannerService {
    private final NavigationDataService navigationDataService;
    private final MultiSpotRoutePlanner routePlanner;

    public ItineraryPlannerService(NavigationDataService navigationDataService,
                                   MultiSpotRoutePlanner routePlanner) {
        this.navigationDataService = navigationDataService;
        this.routePlanner = routePlanner;
    }

    public ItineraryPlannerPreviewResponse preview(ItineraryPlannerPreviewRequest request) {
        ItineraryPlannerPreviewResponse response = new ItineraryPlannerPreviewResponse();
        if (request == null || request.getSpots() == null || request.getSpots().isEmpty()) {
            response.getWarnings().add("No spots selected for planning");
            return response;
        }

        List<ItineraryPlannerPreviewRequest.PlannerSpot> selected = request.getSpots().stream()
                .filter(spot -> spot != null && Boolean.TRUE.equals(spot.getSelected()))
                .toList();
        if (selected.isEmpty()) {
            response.getWarnings().add("No selected spots are enabled");
            return response;
        }

        List<MultiSpotNavigationRequest.SpotVisit> visits = new ArrayList<>();
        int orderIndex = 0;
        for (ItineraryPlannerPreviewRequest.PlannerSpot spot : selected) {
            if (spot.getSpotName() == null || spot.getSpotName().isBlank()) {
                response.getWarnings().add("A selected spot is missing a name");
                continue;
            }
            RoadNode gate = navigationDataService.getGateNode(spot.getSpotName());
            if (gate == null || gate.getOsmid() == null) {
                response.getWarnings().add("No route gate found for " + spot.getSpotName());
                continue;
            }
            String transportMode = normalizeMode(spot.getTransportMode());
            visits.add(new MultiSpotNavigationRequest.SpotVisit(
                    spot.getSpotName(),
                    List.of(gate.getOsmid()),
                    transportMode
            ));
            response.getOrderedSpots().add(new ItineraryPlannerPreviewResponse.OrderedSpot(
                    spot.getSpotId(),
                    spot.getDestinationId(),
                    spot.getSpotName(),
                    spot.getLatitude(),
                    spot.getLongitude(),
                    transportMode,
                    orderIndex++,
                    true
            ));
        }

        response.setDepartureTime(request.getDepartureTime());
        if (visits.size() <= 1) {
            response.setArrivalTime(request.getDepartureTime());
            if (visits.size() == 1) {
                response.getWarnings().add("Only one routable spot selected; route segments were not generated");
            }
            return response;
        }

        MultiSpotNavigationResponse route = routePlanner.plan(new MultiSpotNavigationRequest(
                normalizeStrategy(request.getStrategy()),
                request.getOptimizeVisitOrder(),
                visits
        ));

        List<MultiSpotNavigationResponse.RouteSegment> segments = route == null || route.getSegments() == null
                ? List.of()
                : route.getSegments();
        response.setSegments(segments);
        response.setTotalDistance(safe(route == null ? null : route.getTotalDistance()));
        response.setTotalTime(safe(route == null ? null : route.getTotalTime()));
        if (request.getDepartureTime() != null) {
            response.setArrivalTime(request.getDepartureTime().plusSeconds(Math.round(response.getTotalTime())));
        }
        response.setTimeline(buildTimeline(request.getDepartureTime(), segments));
        return response;
    }

    private List<ItineraryPlannerPreviewResponse.TimelineEntry> buildTimeline(
            LocalDateTime departureTime,
            List<MultiSpotNavigationResponse.RouteSegment> segments) {
        List<ItineraryPlannerPreviewResponse.TimelineEntry> timeline = new ArrayList<>();
        LocalDateTime cursor = departureTime;
        for (MultiSpotNavigationResponse.RouteSegment segment : segments) {
            double duration = safe(segment == null ? null : segment.getTime());
            LocalDateTime start = cursor;
            LocalDateTime end = cursor == null ? null : cursor.plusSeconds(Math.round(duration));
            timeline.add(new ItineraryPlannerPreviewResponse.TimelineEntry(
                    labelFor(segment),
                    segment == null ? null : segment.getType(),
                    segment == null ? null : segment.getFromSpotName(),
                    segment == null ? null : segment.getToSpotName(),
                    safe(segment == null ? null : segment.getDistance()),
                    duration,
                    start,
                    end
            ));
            cursor = end;
        }
        return timeline;
    }

    private String labelFor(MultiSpotNavigationResponse.RouteSegment segment) {
        if (segment == null) {
            return "Route segment";
        }
        if (segment.getFromSpotName() != null
                && segment.getToSpotName() != null
                && !segment.getFromSpotName().equals(segment.getToSpotName())) {
            return segment.getFromSpotName() + " -> " + segment.getToSpotName();
        }
        return segment.getFromSpotName() == null ? "Route segment" : segment.getFromSpotName();
    }

    private String normalizeStrategy(String strategy) {
        return strategy == null || strategy.isBlank() ? "SHORTEST_TIME" : strategy.trim().toUpperCase();
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "walk" : mode.trim().toLowerCase();
    }

    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }
}
