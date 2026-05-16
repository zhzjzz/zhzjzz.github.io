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
    private static final int DEFAULT_STAY_MINUTES = 120;
    private static final double MINUTES_TO_SECONDS = 60.0;

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
            int stayMinutes = normalizeStayMinutes(spot.getStayMinutes());
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
                    stayMinutes,
                    orderIndex++,
                    true
            ));
        }

        response.setDepartureTime(request.getDepartureTime());
        if (visits.size() <= 1) {
            double totalStayTime = totalStayTime(response.getOrderedSpots());
            response.setTotalTime(totalStayTime);
            response.setTimeline(buildTimeline(request.getDepartureTime(), response.getOrderedSpots(), List.of()));
            if (request.getDepartureTime() != null) {
                response.setArrivalTime(request.getDepartureTime().plusSeconds(Math.round(totalStayTime)));
            }
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
        response.setTotalTime(safe(route == null ? null : route.getTotalTime()) + totalStayTime(response.getOrderedSpots()));
        if (request.getDepartureTime() != null) {
            response.setArrivalTime(request.getDepartureTime().plusSeconds(Math.round(response.getTotalTime())));
        }
        response.setTimeline(buildTimeline(request.getDepartureTime(), response.getOrderedSpots(), segments));
        return response;
    }

    private List<ItineraryPlannerPreviewResponse.TimelineEntry> buildTimeline(
            LocalDateTime departureTime,
            List<ItineraryPlannerPreviewResponse.OrderedSpot> orderedSpots,
            List<MultiSpotNavigationResponse.RouteSegment> segments) {
        List<ItineraryPlannerPreviewResponse.TimelineEntry> timeline = new ArrayList<>();
        LocalDateTime cursor = departureTime;
        int segmentIndex = 0;
        for (int spotIndex = 0; spotIndex < orderedSpots.size(); spotIndex++) {
            ItineraryPlannerPreviewResponse.OrderedSpot spot = orderedSpots.get(spotIndex);
            double stayDuration = normalizeStayMinutes(spot.getStayMinutes()) * MINUTES_TO_SECONDS;
            LocalDateTime stayStart = cursor;
            LocalDateTime stayEnd = cursor == null ? null : cursor.plusSeconds(Math.round(stayDuration));
            timeline.add(new ItineraryPlannerPreviewResponse.TimelineEntry(
                    spot.getSpotName() + " stay",
                    "stay",
                    spot.getSpotName(),
                    spot.getSpotName(),
                    0.0,
                    stayDuration,
                    stayStart,
                    stayEnd
            ));
            cursor = stayEnd;

            if (spotIndex >= orderedSpots.size() - 1) {
                continue;
            }
            String currentSpot = spot.getSpotName();
            String nextSpot = orderedSpots.get(spotIndex + 1).getSpotName();
            while (segmentIndex < segments.size()) {
                MultiSpotNavigationResponse.RouteSegment segment = segments.get(segmentIndex++);
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
                if (isRouteBoundary(segment, currentSpot, nextSpot)) {
                    while (segmentIndex < segments.size() && isEnterSegmentFor(segments.get(segmentIndex), nextSpot)) {
                        MultiSpotNavigationResponse.RouteSegment enterSegment = segments.get(segmentIndex++);
                        double enterDuration = safe(enterSegment.getTime());
                        LocalDateTime enterStart = cursor;
                        LocalDateTime enterEnd = cursor == null ? null : cursor.plusSeconds(Math.round(enterDuration));
                        timeline.add(new ItineraryPlannerPreviewResponse.TimelineEntry(
                                labelFor(enterSegment),
                                enterSegment.getType(),
                                enterSegment.getFromSpotName(),
                                enterSegment.getToSpotName(),
                                safe(enterSegment.getDistance()),
                                enterDuration,
                                enterStart,
                                enterEnd
                        ));
                        cursor = enterEnd;
                    }
                    break;
                }
            }
        }
        while (segmentIndex < segments.size()) {
            MultiSpotNavigationResponse.RouteSegment segment = segments.get(segmentIndex++);
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

    private boolean isRouteBoundary(MultiSpotNavigationResponse.RouteSegment segment, String currentSpot, String nextSpot) {
        return segment != null
                && "city".equals(segment.getType())
                && currentSpot.equals(segment.getFromSpotName())
                && nextSpot.equals(segment.getToSpotName());
    }

    private boolean isEnterSegmentFor(MultiSpotNavigationResponse.RouteSegment segment, String spotName) {
        return segment != null
                && "spot_enter".equals(segment.getType())
                && spotName.equals(segment.getToSpotName());
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

    private int normalizeStayMinutes(Integer stayMinutes) {
        return stayMinutes == null || stayMinutes < 0 ? DEFAULT_STAY_MINUTES : stayMinutes;
    }

    private double totalStayTime(List<ItineraryPlannerPreviewResponse.OrderedSpot> orderedSpots) {
        return orderedSpots.stream()
                .mapToDouble(spot -> normalizeStayMinutes(spot.getStayMinutes()) * MINUTES_TO_SECONDS)
                .sum();
    }

    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }
}
