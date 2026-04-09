package com.travel.system.controller;

import com.travel.system.dto.RoutePlanRequest;
import com.travel.system.dto.RoutePlanResponse;
import com.travel.system.service.RoutePlanningService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RoutePlanningService routePlanningService;

    public RouteController(RoutePlanningService routePlanningService) {
        this.routePlanningService = routePlanningService;
    }

    @PostMapping("/plan")
    public RoutePlanResponse plan(@RequestBody RoutePlanRequest request) {
        String strategy = request.getStrategy() == null ? "distance" : request.getStrategy();
        String transport = request.getTransport() == null ? "walk" : request.getTransport();
        return routePlanningService.shortestPath(request.getFromNodeId(), request.getToNodeId(), strategy, transport);
    }
}
