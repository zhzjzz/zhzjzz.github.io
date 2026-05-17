# Itinerary One-Click Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reusable one-click planner that converts collaborative itinerary spots into a route preview with distance, duration, optional departure time, and estimated timeline.

**Architecture:** Add a backend planner preview endpoint that accepts normalized real spots and planning options, resolves spots to navigation visits, and reuses the existing multi-spot route planner. Add frontend planner utilities, a reusable planner panel, and a collaboration-page adapter so the same module can later power one-click itinerary replication.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis/SQLite data, JUnit 5, Mockito, Vue 3 Composition API, Element Plus, Axios, Node built-in test runner.

---

## Scope Check

This is one cohesive feature across backend planner preview, frontend reusable planning UI, and documentation. It should be implemented in the existing `codex/tactical-map-collaboration` worktree because that branch already contains real collaborative map spots and voting.

In scope:

- `POST /api/itinerary-planner/preview`.
- Optional `departureTime`.
- Automatic default selection of `must` and `want` consensus spots.
- Manual include/exclude and order controls in the frontend planner panel.
- Reusable frontend module for future one-click itinerary replication.
- Documentation that states itinerary replication can reuse this module.

Out of scope:

- Persisting generated plans.
- Multi-day scheduling.
- Hotels, dining, rest stops, or live traffic.
- Replacing the standalone `/routes` page.

## File Structure

Backend:

- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewRequest.java`: request body for reusable itinerary planning.
- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewResponse.java`: ordered spots, route segments, totals, absolute timing, warnings.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java`: reusable wrapper around the existing multi-spot route logic. Move `NavigationController.multiSpotRoute(...)` implementation and its private helper methods here, preserving behavior.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/controller/nav/NavigationController.java`: delegate `/multi-spot` to `MultiSpotRoutePlanner`.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryPlannerService.java`: normalizes planner spots, resolves route visits, calls `MultiSpotRoutePlanner`, builds timeline.
- Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryPlannerController.java`: exposes `/api/itinerary-planner/preview`.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryPlannerServiceTest.java`: timing, default gate resolution, warning behavior.
- Create `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryPlannerControllerTest.java`: endpoint smoke test with fake service.
- Modify `data-structure-design-backend/src/test/java/com/travel/system/controller/nav/NavigationControllerTest.java`: construct controller with `MultiSpotRoutePlanner`.

Frontend:

- Modify `data-structure-design-frontend/src/api/travel.js`: add `previewItineraryPlan`.
- Create `data-structure-design-frontend/src/utils/itineraryPlanner.js`: reusable planner input builder and formatters.
- Create `data-structure-design-frontend/src/utils/itineraryPlanner.test.js`: default selection, payload, time formatting tests.
- Create `data-structure-design-frontend/src/composables/useItineraryPlanner.js`: reusable request state and planner submission.
- Create `data-structure-design-frontend/src/components/itinerary/ItineraryPlannerPanel.vue`: reusable UI panel for planning options and preview result.
- Modify `data-structure-design-frontend/src/views/ItineraryView.vue`: add one-click planner action and pass `tacticalNodes` into the panel.
- Modify `docs/demo-runbook.md`: add one-click planner demo steps and replication reuse note.

---

### Task 1: Backend Planner Contract

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewRequest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewResponse.java`

- [ ] **Step 1: Add preview request DTO**

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewRequest.java`:

```java
package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlannerPreviewRequest {
    private LocalDateTime departureTime;
    private String strategy;
    private Boolean optimizeVisitOrder;
    private List<PlannerSpot> spots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlannerSpot {
        private Long spotId;
        private Long destinationId;
        private String spotName;
        private Double latitude;
        private Double longitude;
        private String transportMode;
        private Boolean selected;
    }
}
```

- [ ] **Step 2: Add preview response DTO**

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewResponse.java`:

```java
package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryPlannerPreviewResponse {
    private List<OrderedSpot> orderedSpots = new ArrayList<>();
    private List<MultiSpotNavigationResponse.RouteSegment> segments = new ArrayList<>();
    private Double totalDistance = 0.0;
    private Double totalTime = 0.0;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private List<TimelineEntry> timeline = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderedSpot {
        private Long spotId;
        private Long destinationId;
        private String spotName;
        private Double latitude;
        private Double longitude;
        private String transportMode;
        private Integer orderIndex;
        private Boolean routable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry {
        private String label;
        private String type;
        private String fromSpotName;
        private String toSpotName;
        private Double distance;
        private Double duration;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
```

- [ ] **Step 3: Compile DTOs**

Run:

```powershell
cd data-structure-design-backend
$env:JAVA_HOME='D:\software\jdk-26'
$env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
mvn.cmd test -DskipTests
```

Expected: build reaches compile/testCompile without DTO errors.

- [ ] **Step 4: Commit contract**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewRequest.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryPlannerPreviewResponse.java
git commit -m "feat: add itinerary planner preview contract"
```

---

### Task 2: Extract Reusable Multi-Spot Route Planner

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/nav/NavigationController.java`
- Modify: `data-structure-design-backend/src/test/java/com/travel/system/controller/nav/NavigationControllerTest.java`

- [ ] **Step 1: Create reusable planner service**

Create `data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java`.

Use this structure, then move the existing `NavigationController.multiSpotRoute(...)` method body and every private helper it depends on into this service:

```java
package com.travel.system.service.nav;

import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.dto.NavigationResponse;
import com.travel.system.model.nav.CityRoute;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
    private static final int EXACT_ORDER_LIMIT = 10;
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
        // Move the exact existing NavigationController.multiSpotRoute method body here.
    }

    // Move these helper methods from NavigationController into this class:
    // shortestDistancePath(...)
    // shortestTimePath(...)
    // mixedTransportPath(...)
    // dijkstra(...)
    // normalizeMode(...)
    // normalizeModes(...)
    // planByStrategy(...)
    // resolveVisitNodes(...)
    // orderVisitNodes(...)
    // exactBestOrder(...)
    // approximateBestOrder(...)
    // buildCostMatrix(...)
    // buildAnchorCost(...)
    // nearestNeighborOrder(...)
    // improveWithTwoOpt(...)
    // orderCost(...)
    // createInnerSegment(...)
    // createCitySegment(...)
    // cityRouteDistanceMeters(...)
    // cityRouteTimeSeconds(...)
    // safe(...)
    // bestMixedMode(...)
    // sameSpot(...)
    // normalizeSpotName(...)
    // edgeLength(...)
    // EdgeWeight
    // EdgeMode
    // NodeState
    // MixedChoice
}
```

Important implementation detail: remove the migration notes after moving the actual helper code. The final service must compile with real method bodies.

- [ ] **Step 2: Replace NavigationController multi-spot logic with delegation**

Modify `data-structure-design-backend/src/main/java/com/travel/system/controller/nav/NavigationController.java`:

```java
private final MultiSpotRoutePlanner multiSpotRoutePlanner;

public NavigationController(NavigationDataService navigationDataService,
                            TransportModeService transportModeService,
                            CityRouteService cityRouteService,
                            MultiSpotRoutePlanner multiSpotRoutePlanner) {
    this.navigationDataService = navigationDataService;
    this.transportModeService = transportModeService;
    this.cityRouteService = cityRouteService;
    this.multiSpotRoutePlanner = multiSpotRoutePlanner;
}

@Operation(summary = "澶氭櫙鍖哄鍦扮偣璺嚎瑙勫垝")
@PostMapping("/multi-spot")
public MultiSpotNavigationResponse multiSpotRoute(@RequestBody MultiSpotNavigationRequest request) {
    return multiSpotRoutePlanner.plan(request);
}
```

After this change, delete the duplicate private helpers from `NavigationController` only after confirming no remaining controller method uses them. Keep helpers still used by `/plan` and `/cross-spot` in the controller.

- [ ] **Step 3: Update NavigationControllerTest construction**

Modify `data-structure-design-backend/src/test/java/com/travel/system/controller/nav/NavigationControllerTest.java`:

```java
TransportModeService transportModeService = new TransportModeService();
MultiSpotRoutePlanner planner = new MultiSpotRoutePlanner(
        navigationDataService,
        transportModeService,
        cityRouteService
);
NavigationController controller = new NavigationController(
        navigationDataService,
        transportModeService,
        cityRouteService,
        planner
);
```

- [ ] **Step 4: Run navigation controller test**

Run:

```powershell
mvn.cmd test -Dtest=NavigationControllerTest
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit reusable route planner**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java data-structure-design-backend/src/main/java/com/travel/system/controller/nav/NavigationController.java data-structure-design-backend/src/test/java/com/travel/system/controller/nav/NavigationControllerTest.java
git commit -m "refactor: extract multi spot route planner"
```

---

### Task 3: Backend Itinerary Planner Preview Service and API

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryPlannerService.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryPlannerController.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryPlannerServiceTest.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryPlannerControllerTest.java`

- [ ] **Step 1: Write failing service tests**

Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryPlannerServiceTest.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.dto.MultiSpotNavigationRequest;
import com.travel.system.dto.MultiSpotNavigationResponse;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.service.nav.MultiSpotRoutePlanner;
import com.travel.system.service.nav.NavigationDataService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryPlannerServiceTest {
    @Test
    void previewBuildsArrivalTimeAndTimelineFromRouteSegments() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        MultiSpotRoutePlanner routePlanner = mock(MultiSpotRoutePlanner.class);
        ItineraryPlannerService service = new ItineraryPlannerService(navigationDataService, routePlanner);

        when(navigationDataService.getGateNode("Spot A"))
                .thenReturn(new RoadNode(101L, "Spot A", 39.9, 116.3, null, null));
        when(navigationDataService.getGateNode("Spot B"))
                .thenReturn(new RoadNode(202L, "Spot B", 39.95, 116.4, null, null));

        MultiSpotNavigationResponse route = new MultiSpotNavigationResponse();
        route.setTotalDistance(1200.0);
        route.setTotalTime(1800.0);
        route.setSegments(List.of(new MultiSpotNavigationResponse.RouteSegment(
                "city",
                "Spot A",
                "Spot B",
                101L,
                202L,
                "walk",
                "drive",
                List.of(),
                new double[]{39.9, 116.3},
                new double[]{39.95, 116.4},
                1200.0,
                1800.0
        )));
        when(routePlanner.plan(any(MultiSpotNavigationRequest.class))).thenReturn(route);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        request.setDepartureTime(LocalDateTime.of(2026, 5, 15, 9, 0));
        request.setStrategy("SHORTEST_TIME");
        request.setOptimizeVisitOrder(true);
        request.setSpots(List.of(
                new ItineraryPlannerPreviewRequest.PlannerSpot(1L, 1L, "Spot A", 39.9, 116.3, "walk", true),
                new ItineraryPlannerPreviewRequest.PlannerSpot(2L, 2L, "Spot B", 39.95, 116.4, "walk", true)
        ));

        ItineraryPlannerPreviewResponse response = service.preview(request);

        assertThat(response.getTotalDistance()).isEqualTo(1200.0);
        assertThat(response.getTotalTime()).isEqualTo(1800.0);
        assertThat(response.getArrivalTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 9, 30));
        assertThat(response.getTimeline()).hasSize(1);
        assertThat(response.getTimeline().get(0).getStartTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 9, 0));
        assertThat(response.getTimeline().get(0).getEndTime()).isEqualTo(LocalDateTime.of(2026, 5, 15, 9, 30));
        assertThat(response.getOrderedSpots()).extracting(ItineraryPlannerPreviewResponse.OrderedSpot::getSpotName)
                .containsExactly("Spot A", "Spot B");
    }

    @Test
    void previewSkipsUnroutableSpotsAndReturnsWarnings() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        MultiSpotRoutePlanner routePlanner = mock(MultiSpotRoutePlanner.class);
        ItineraryPlannerService service = new ItineraryPlannerService(navigationDataService, routePlanner);

        when(navigationDataService.getGateNode("Missing Spot")).thenReturn(null);
        when(navigationDataService.getGateNode("Spot B"))
                .thenReturn(new RoadNode(202L, "Spot B", 39.95, 116.4, null, null));

        MultiSpotNavigationResponse emptyRoute = new MultiSpotNavigationResponse();
        emptyRoute.setSegments(List.of());
        emptyRoute.setTotalDistance(0.0);
        emptyRoute.setTotalTime(0.0);
        when(routePlanner.plan(any(MultiSpotNavigationRequest.class))).thenReturn(emptyRoute);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        request.setSpots(List.of(
                new ItineraryPlannerPreviewRequest.PlannerSpot(1L, 1L, "Missing Spot", 39.9, 116.3, "walk", true),
                new ItineraryPlannerPreviewRequest.PlannerSpot(2L, 2L, "Spot B", 39.95, 116.4, "walk", true)
        ));

        ItineraryPlannerPreviewResponse response = service.preview(request);

        assertThat(response.getOrderedSpots()).hasSize(1);
        assertThat(response.getOrderedSpots().get(0).getSpotName()).isEqualTo("Spot B");
        assertThat(response.getWarnings()).anyMatch(warning -> warning.contains("Missing Spot"));
    }
}
```

- [ ] **Step 2: Run service tests to verify failure**

Run:

```powershell
mvn.cmd test -Dtest=ItineraryPlannerServiceTest
```

Expected: compilation fails because `ItineraryPlannerService` does not exist.

- [ ] **Step 3: Implement service**

Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryPlannerService.java`:

```java
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
            visits.add(new MultiSpotNavigationRequest.SpotVisit(
                    spot.getSpotName(),
                    List.of(gate.getOsmid()),
                    normalizeMode(spot.getTransportMode())
            ));
            response.getOrderedSpots().add(new ItineraryPlannerPreviewResponse.OrderedSpot(
                    spot.getSpotId(),
                    spot.getDestinationId(),
                    spot.getSpotName(),
                    spot.getLatitude(),
                    spot.getLongitude(),
                    normalizeMode(spot.getTransportMode()),
                    orderIndex++,
                    true
            ));
        }

        if (visits.size() <= 1) {
            response.setDepartureTime(request.getDepartureTime());
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

        response.setSegments(route.getSegments() == null ? List.of() : route.getSegments());
        response.setTotalDistance(safe(route.getTotalDistance()));
        response.setTotalTime(safe(route.getTotalTime()));
        response.setDepartureTime(request.getDepartureTime());
        if (request.getDepartureTime() != null) {
            response.setArrivalTime(request.getDepartureTime().plusSeconds(Math.round(response.getTotalTime())));
        }
        response.setTimeline(buildTimeline(request.getDepartureTime(), response.getSegments()));
        return response;
    }

    private List<ItineraryPlannerPreviewResponse.TimelineEntry> buildTimeline(
            LocalDateTime departureTime,
            List<MultiSpotNavigationResponse.RouteSegment> segments) {
        List<ItineraryPlannerPreviewResponse.TimelineEntry> timeline = new ArrayList<>();
        LocalDateTime cursor = departureTime;
        for (MultiSpotNavigationResponse.RouteSegment segment : segments) {
            double duration = safe(segment.getTime());
            LocalDateTime start = cursor;
            LocalDateTime end = cursor == null ? null : cursor.plusSeconds(Math.round(duration));
            timeline.add(new ItineraryPlannerPreviewResponse.TimelineEntry(
                    labelFor(segment),
                    segment.getType(),
                    segment.getFromSpotName(),
                    segment.getToSpotName(),
                    safe(segment.getDistance()),
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
        if (segment.getFromSpotName() != null && segment.getToSpotName() != null
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
```

- [ ] **Step 4: Add controller**

Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryPlannerController.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.service.ItineraryPlannerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-planner")
public class ItineraryPlannerController {
    private final ItineraryPlannerService plannerService;

    public ItineraryPlannerController(ItineraryPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping("/preview")
    public ItineraryPlannerPreviewResponse preview(@RequestBody ItineraryPlannerPreviewRequest request) {
        return plannerService.preview(request);
    }
}
```

- [ ] **Step 5: Add controller smoke test**

Create `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryPlannerControllerTest.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.service.ItineraryPlannerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryPlannerControllerTest {
    @Test
    void previewDelegatesToPlannerService() {
        ItineraryPlannerService service = mock(ItineraryPlannerService.class);
        ItineraryPlannerController controller = new ItineraryPlannerController(service);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        ItineraryPlannerPreviewResponse response = new ItineraryPlannerPreviewResponse();
        response.setWarnings(List.of("sample"));
        when(service.preview(request)).thenReturn(response);

        assertThat(controller.preview(request).getWarnings()).containsExactly("sample");
    }
}
```

- [ ] **Step 6: Run planner backend tests**

Run:

```powershell
mvn.cmd test -Dtest=ItineraryPlannerServiceTest,ItineraryPlannerControllerTest,NavigationControllerTest
```

Expected: all listed tests pass.

- [ ] **Step 7: Commit backend planner API**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryPlannerService.java data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryPlannerController.java data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryPlannerServiceTest.java data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryPlannerControllerTest.java
git commit -m "feat: add itinerary planner preview api"
```

---

### Task 4: Frontend Planner Utilities and API

**Files:**
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Create: `data-structure-design-frontend/src/utils/itineraryPlanner.js`
- Create: `data-structure-design-frontend/src/utils/itineraryPlanner.test.js`
- Create: `data-structure-design-frontend/src/composables/useItineraryPlanner.js`

- [ ] **Step 1: Add utility tests**

Create `data-structure-design-frontend/src/utils/itineraryPlanner.test.js`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildPlannerPayload,
  defaultPlannerSelection,
  formatDuration,
  formatKm,
} from './itineraryPlanner.js'

test('defaultPlannerSelection selects must and want nodes', () => {
  const selected = defaultPlannerSelection([
    { spotId: 1, spotName: 'A', consensus: 'must' },
    { spotId: 2, spotName: 'B', consensus: 'want' },
    { spotId: 3, spotName: 'C', consensus: 'backup' },
  ])

  assert.deepEqual(selected.map((spot) => [spot.spotId, spot.selected]), [
    [1, true],
    [2, true],
    [3, false],
  ])
})

test('defaultPlannerSelection selects all nodes when no must or want exists', () => {
  const selected = defaultPlannerSelection([
    { spotId: 1, spotName: 'A', consensus: 'backup' },
    { spotId: 2, spotName: 'B', consensus: 'conflict' },
  ])

  assert.deepEqual(selected.map((spot) => spot.selected), [true, true])
})

test('buildPlannerPayload keeps selected spots in order', () => {
  const payload = buildPlannerPayload({
    departureTime: '2026-05-15T09:00',
    strategy: 'SHORTEST_TIME',
    optimizeVisitOrder: false,
    spots: [
      { spotId: 1, destinationId: 1, spotName: 'A', latitude: 1, longitude: 2, selected: false },
      { spotId: 2, destinationId: 2, spotName: 'B', latitude: 3, longitude: 4, selected: true },
    ],
  })

  assert.equal(payload.departureTime, '2026-05-15T09:00')
  assert.equal(payload.optimizeVisitOrder, false)
  assert.equal(payload.spots.length, 2)
  assert.equal(payload.spots[0].selected, false)
  assert.equal(payload.spots[1].spotName, 'B')
  assert.equal(payload.spots[1].transportMode, 'walk')
})

test('format helpers render compact distance and duration', () => {
  assert.equal(formatKm(1530), '1.53 km')
  assert.equal(formatDuration(3660), '1 小时 1 分钟')
})
```

- [ ] **Step 2: Run utility tests to verify failure**

Run:

```powershell
cd data-structure-design-frontend
npm.cmd test
```

Expected: fails because `itineraryPlanner.js` does not exist.

- [ ] **Step 3: Implement planner utilities**

Create `data-structure-design-frontend/src/utils/itineraryPlanner.js`:

```js
const AGREED_CONSENSUS = new Set(['must', 'want'])

export const defaultPlannerSelection = (nodes = []) => {
  const hasAgreed = nodes.some((node) => AGREED_CONSENSUS.has(node.consensus))
  return nodes.map((node, index) => ({
    spotId: node.spotId,
    destinationId: node.destinationId,
    spotName: node.spotName,
    latitude: node.latitude,
    longitude: node.longitude,
    consensus: node.consensus,
    transportMode: node.transportMode || 'walk',
    selected: hasAgreed ? AGREED_CONSENSUS.has(node.consensus) : true,
    orderIndex: index,
  }))
}

export const buildPlannerPayload = ({
  departureTime = '',
  strategy = 'SHORTEST_TIME',
  optimizeVisitOrder = true,
  spots = [],
} = {}) => ({
  departureTime: departureTime || null,
  strategy,
  optimizeVisitOrder,
  spots: spots.map((spot) => ({
    spotId: spot.spotId,
    destinationId: spot.destinationId,
    spotName: spot.spotName,
    latitude: spot.latitude,
    longitude: spot.longitude,
    transportMode: spot.transportMode || 'walk',
    selected: Boolean(spot.selected),
  })),
})

export const selectedPlannerCount = (spots = []) => (
  spots.filter((spot) => spot.selected).length
)

export const formatKm = (meters = 0) => `${((Number(meters) || 0) / 1000).toFixed(2)} km`

export const formatDuration = (seconds = 0) => {
  const totalMinutes = Math.round((Number(seconds) || 0) / 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours && minutes) return `${hours} 小时 ${minutes} 分钟`
  if (hours) return `${hours} 小时`
  return `${minutes} 分钟`
}

export const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
```

- [ ] **Step 4: Add API helper**

Modify `data-structure-design-frontend/src/api/travel.js` below itinerary map spot helpers:

```js
export const previewItineraryPlan = (payload) => http.post('/itinerary-planner/preview', payload)
```

- [ ] **Step 5: Add planner composable**

Create `data-structure-design-frontend/src/composables/useItineraryPlanner.js`:

```js
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { previewItineraryPlan } from '../api/travel'
import { buildPlannerPayload, selectedPlannerCount } from '../utils/itineraryPlanner'

export const useItineraryPlanner = () => {
  const loading = ref(false)
  const preview = ref(null)
  const error = ref('')

  const generatePreview = async (options) => {
    error.value = ''
    const payload = buildPlannerPayload(options)
    if (selectedPlannerCount(payload.spots) < 1) {
      error.value = '请选择至少一个景点'
      ElMessage.warning(error.value)
      return null
    }
    loading.value = true
    try {
      const { data } = await previewItineraryPlan(payload)
      preview.value = data
      return data
    } catch (caught) {
      error.value = caught?.response?.data?.message || caught?.message || '一键规划失败'
      ElMessage.error(error.value)
      return null
    } finally {
      loading.value = false
    }
  }

  const resetPreview = () => {
    preview.value = null
    error.value = ''
  }

  return {
    loading,
    preview,
    error,
    generatePreview,
    resetPreview,
  }
}
```

- [ ] **Step 6: Run frontend tests**

Run:

```powershell
npm.cmd test
```

Expected: all Node tests pass.

- [ ] **Step 7: Commit frontend utility layer**

```powershell
git add data-structure-design-frontend/src/api/travel.js data-structure-design-frontend/src/utils/itineraryPlanner.js data-structure-design-frontend/src/utils/itineraryPlanner.test.js data-structure-design-frontend/src/composables/useItineraryPlanner.js
git commit -m "feat: add itinerary planner frontend utilities"
```

---

### Task 5: Reusable Planner Panel

**Files:**
- Create: `data-structure-design-frontend/src/components/itinerary/ItineraryPlannerPanel.vue`

- [ ] **Step 1: Create planner panel component**

Create `data-structure-design-frontend/src/components/itinerary/ItineraryPlannerPanel.vue`:

```vue
<script setup>
import { computed, ref, watch } from 'vue'
import { Calendar, Down, PlayOne, Up } from '@icon-park/vue-next'
import { useItineraryPlanner } from '../../composables/useItineraryPlanner'
import {
  defaultPlannerSelection,
  formatDateTime,
  formatDuration,
  formatKm,
  selectedPlannerCount,
} from '../../utils/itineraryPlanner'

const props = defineProps({
  spots: {
    type: Array,
    default: () => [],
  },
})

const plannerSpots = ref([])
const departureTime = ref('')
const strategy = ref('SHORTEST_TIME')
const optimizeVisitOrder = ref(true)

const { loading, preview, error, generatePreview, resetPreview } = useItineraryPlanner()

const selectedCount = computed(() => selectedPlannerCount(plannerSpots.value))
const canPlan = computed(() => selectedCount.value >= 1)

watch(() => props.spots, (spots) => {
  plannerSpots.value = defaultPlannerSelection(spots)
  resetPreview()
}, { immediate: true, deep: true })

const moveSpot = (index, delta) => {
  const next = index + delta
  if (next < 0 || next >= plannerSpots.value.length) return
  const copy = [...plannerSpots.value]
  const [item] = copy.splice(index, 1)
  copy.splice(next, 0, item)
  plannerSpots.value = copy.map((spot, orderIndex) => ({ ...spot, orderIndex }))
}

const submit = async () => {
  await generatePreview({
    departureTime: departureTime.value,
    strategy: strategy.value,
    optimizeVisitOrder: optimizeVisitOrder.value,
    spots: plannerSpots.value,
  })
}
</script>

<template>
  <section class="planner-panel">
    <header class="planner-head">
      <div>
        <strong>一键规划</strong>
        <span>{{ selectedCount }} / {{ plannerSpots.length }} 个景点</span>
      </div>
      <el-button type="primary" :loading="loading" :disabled="!canPlan" @click="submit">
        <PlayOne theme="outline" size="16" fill="currentColor" />
        生成
      </el-button>
    </header>

    <div class="planner-controls">
      <el-date-picker
        v-model="departureTime"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="出发时间"
        class="planner-date"
      />
      <el-select v-model="strategy" class="planner-select">
        <el-option label="最短时间" value="SHORTEST_TIME" />
        <el-option label="最短距离" value="SHORTEST_DISTANCE" />
      </el-select>
      <el-switch v-model="optimizeVisitOrder" active-text="自动排序" inactive-text="手动顺序" />
    </div>

    <div class="planner-spots">
      <article v-for="(spot, index) in plannerSpots" :key="spot.spotId" class="planner-spot">
        <el-checkbox v-model="spot.selected" />
        <div>
          <strong>{{ spot.spotName }}</strong>
          <span>{{ spot.consensus || 'backup' }}</span>
        </div>
        <div class="spot-actions">
          <el-button text :disabled="index === 0" @click="moveSpot(index, -1)">
            <Up theme="outline" size="14" fill="currentColor" />
          </el-button>
          <el-button text :disabled="index === plannerSpots.length - 1" @click="moveSpot(index, 1)">
            <Down theme="outline" size="14" fill="currentColor" />
          </el-button>
        </div>
      </article>
    </div>

    <el-alert v-if="error" type="warning" :title="error" show-icon :closable="false" />

    <section v-if="preview" class="planner-preview">
      <div class="preview-metrics">
        <article>
          <span>总距离</span>
          <strong>{{ formatKm(preview.totalDistance) }}</strong>
        </article>
        <article>
          <span>总耗时</span>
          <strong>{{ formatDuration(preview.totalTime) }}</strong>
        </article>
        <article>
          <span><Calendar theme="outline" size="14" fill="currentColor" /> 抵达</span>
          <strong>{{ formatDateTime(preview.arrivalTime) }}</strong>
        </article>
      </div>

      <div v-if="preview.warnings?.length" class="planner-warnings">
        <el-tag v-for="warning in preview.warnings" :key="warning" type="warning" effect="plain">
          {{ warning }}
        </el-tag>
      </div>

      <div class="timeline-list">
        <article v-for="(item, index) in preview.timeline || []" :key="`${item.label}-${index}`" class="timeline-item">
          <strong>{{ index + 1 }}. {{ item.label }}</strong>
          <span>{{ formatDuration(item.duration) }} · {{ formatKm(item.distance) }}</span>
          <small v-if="item.startTime || item.endTime">
            {{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}
          </small>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.planner-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.planner-head,
.planner-controls,
.planner-spot,
.spot-actions,
.preview-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
}

.planner-head {
  justify-content: space-between;
}

.planner-head strong,
.planner-head span {
  display: block;
}

.planner-head strong {
  color: #111827;
  font-size: 15px;
  font-weight: 900;
}

.planner-head span,
.planner-spot span,
.timeline-item span,
.timeline-item small {
  color: #64748b;
  font-size: 12px;
}

.planner-controls {
  flex-wrap: wrap;
}

.planner-date {
  max-width: 210px;
}

.planner-select {
  width: 130px;
}

.planner-spots,
.planner-preview,
.timeline-list,
.planner-warnings {
  display: grid;
  gap: 8px;
}

.planner-spot {
  min-height: 48px;
  padding: 9px 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #f8fafc;
}

.planner-spot > div:nth-child(2) {
  min-width: 0;
  flex: 1;
}

.planner-spot strong {
  display: block;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-metrics {
  align-items: stretch;
}

.preview-metrics article {
  flex: 1;
  min-width: 0;
  padding: 10px;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
}

.preview-metrics span,
.preview-metrics strong {
  display: block;
}

.preview-metrics span {
  color: #cbd5e1;
  font-size: 12px;
}

.preview-metrics strong {
  margin-top: 4px;
  font-size: 15px;
}

.planner-warnings {
  justify-items: start;
}

.timeline-item {
  padding: 10px;
  border-left: 3px solid #ff385c;
  border-radius: 8px;
  background: #fff7f8;
}

.timeline-item strong,
.timeline-item span,
.timeline-item small {
  display: block;
}

.timeline-item strong {
  color: #111827;
  font-size: 13px;
}
</style>
```

- [ ] **Step 2: Run frontend build**

Run:

```powershell
npm.cmd run build
```

Expected: Vite build passes.

- [ ] **Step 3: Clean generated dist assets if build changes tracked files**

Run:

```powershell
git status --short
```

If `data-structure-design-frontend/dist/index.html` or new `dist/assets/index-*.js/css` files changed, revert only generated `dist` changes. Do not revert source files.

- [ ] **Step 4: Commit planner panel**

```powershell
git add data-structure-design-frontend/src/components/itinerary/ItineraryPlannerPanel.vue
git commit -m "feat: add reusable itinerary planner panel"
```

---

### Task 6: Collaboration Drawer Integration

**Files:**
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`

- [ ] **Step 1: Import planner panel**

Modify `data-structure-design-frontend/src/views/ItineraryView.vue` imports:

```js
import ItineraryPlannerPanel from '../components/itinerary/ItineraryPlannerPanel.vue'
```

- [ ] **Step 2: Add planner panel state**

Add near existing collaboration refs:

```js
const plannerOpen = ref(false)
```

- [ ] **Step 3: Reset planner on collaboration lifecycle**

In `openCollaboration`, after clearing `spotSearchResults.value`, add:

```js
plannerOpen.value = false
```

In `closeCollaboration`, add:

```js
plannerOpen.value = false
```

- [ ] **Step 4: Add one-click planner action to drawer**

Inside the `.tactical-layout` block in `ItineraryView.vue`, add a toolbar above `TacticalMapPanel` by wrapping the map column:

```vue
<div class="tactical-map-stack">
  <div class="planner-action-row">
    <span>{{ tacticalNodes.length }} 个真实景点</span>
    <el-button type="primary" :disabled="!tacticalNodes.length" @click="plannerOpen = !plannerOpen">
      一键规划
    </el-button>
  </div>
  <TacticalMapPanel
    :nodes="tacticalNodes"
    :selected-spot-id="selectedSpotId"
    @select-node="selectNode"
  />
</div>
```

Replace the existing direct `TacticalMapPanel` instance, not the whole layout.

- [ ] **Step 5: Add planner panel to side column**

Inside `.tactical-side`, before `ConsensusProgress`, add:

```vue
<ItineraryPlannerPanel
  v-if="plannerOpen"
  :spots="tacticalNodes"
/>
```

- [ ] **Step 6: Add scoped styles**

Add to the `<style scoped>` section in `ItineraryView.vue`:

```css
.tactical-map-stack {
  display: grid;
  gap: 10px;
}

.planner-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.planner-action-row span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}
```

- [ ] **Step 7: Run frontend verification**

Run:

```powershell
npm.cmd test
npm.cmd run build
```

Expected: tests pass and Vite build passes.

- [ ] **Step 8: Clean generated dist assets if needed**

Run:

```powershell
git status --short
```

If Vite changed generated `dist` files, remove only the new generated assets and restore `dist/index.html` to the committed asset names.

- [ ] **Step 9: Commit collaboration integration**

```powershell
git add data-structure-design-frontend/src/views/ItineraryView.vue
git commit -m "feat: connect planner panel to collaboration drawer"
```

---

### Task 7: Documentation and End-to-End Verification

**Files:**
- Modify: `docs/demo-runbook.md`

- [ ] **Step 1: Update runbook**

Modify `docs/demo-runbook.md` under `## Tactical Map Collaboration Demo` by adding these steps after adding and voting on real destinations:

```markdown
12. Click `一键规划`.
13. Confirm the planner panel preselects `must` and `want` spots.
14. Choose a departure time and keep automatic ordering enabled.
15. Click `生成`.
16. Confirm the preview shows total distance, total travel time, estimated arrival time, and segment timeline.
17. Note that future one-click itinerary replication can reuse the same `POST /api/itinerary-planner/preview` endpoint and planner panel by passing replicated destination spots into the same input shape.
```

- [ ] **Step 2: Run backend full verification**

Run:

```powershell
cd data-structure-design-backend
$env:JAVA_HOME='D:\software\jdk-26'
$env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
mvn.cmd test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run frontend full verification**

Run:

```powershell
cd data-structure-design-frontend
npm.cmd test
npm.cmd run build
```

Expected: all frontend tests pass and Vite build completes.

- [ ] **Step 4: Verify planner API manually**

With backend running, call:

```powershell
$body = @{
  departureTime = '2026-05-15T09:00:00'
  strategy = 'SHORTEST_TIME'
  optimizeVisitOrder = $true
  spots = @(
    @{
      spotId = 130
      destinationId = 130
      spotName = '中国航空博物馆'
      latitude = 40.18030374722222
      longitude = 116.35033976111112
      transportMode = 'walk'
      selected = $true
    }
  )
} | ConvertTo-Json -Depth 5
Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/api/itinerary-planner/preview' -Method Post -ContentType 'application/json' -Body $body
```

Expected: HTTP 200 with `orderedSpots`, `totalTime`, `timeline`, and `warnings` fields.

- [ ] **Step 5: Commit docs**

```powershell
git add docs/demo-runbook.md
git commit -m "docs: add itinerary one-click planner demo"
```

---

## Self-Review

Spec coverage:

- One-click planner from collaboration spots: Tasks 5 and 6.
- Default `must`/`want`, fallback to all spots: Task 4 utility tests and implementation.
- Optional departure time and arrival estimate: Task 3 service tests and implementation.
- Reusable backend contract: Tasks 1 and 3.
- Reusable frontend panel/composable: Tasks 4 and 5.
- Future itinerary replication reuse documented: Task 7.

Placeholder scan:

- No incomplete work markers remain.
- The one extraction step references existing code movement, but names every method that must move and requires final code to compile with real method bodies.

Type consistency:

- Request uses `departureTime`, `strategy`, `optimizeVisitOrder`, and `spots`.
- Planner spots use `spotId`, `destinationId`, `spotName`, `latitude`, `longitude`, `transportMode`, and `selected`.
- Response uses `orderedSpots`, `segments`, `totalDistance`, `totalTime`, `departureTime`, `arrivalTime`, `timeline`, and `warnings`.
- Frontend payload names match backend DTO names.
