package com.travel.system.controller;

import com.travel.system.dto.RoutePlanRequest;
import com.travel.system.dto.RoutePlanResponse;
import com.travel.system.service.RoutePlanningService;
import org.springframework.web.bind.annotation.*;

/**
 * 路线规划控制器。
 * 对外提供统一路径规划入口，支持不同策略和交通工具参数。
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RoutePlanningService routePlanningService;

    public RouteController(RoutePlanningService routePlanningService) {
        this.routePlanningService = routePlanningService;
    }

    /**
     * 单次路线规划接口。
     * 若前端未传策略/交通工具，将自动回落到默认值，避免空参数导致异常。
     */
    @PostMapping("/plan")
    public RoutePlanResponse plan(@RequestBody RoutePlanRequest request) {
        String strategy = request.getStrategy() == null ? "distance" : request.getStrategy();
        String transport = request.getTransport() == null ? "walk" : request.getTransport();
        return routePlanningService.shortestPath(request.getFromNodeId(), request.getToNodeId(), strategy, transport);
    }
}
