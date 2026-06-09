package com.travel.system.controller.nav;

import com.travel.system.dto.IndoorBuildingDemo;
import com.travel.system.dto.IndoorNavigationRequest;
import com.travel.system.dto.IndoorNavigationResponse;
import com.travel.system.service.nav.IndoorNavigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nav/indoor")
@Tag(name = "室内导航演示", description = "教学楼、博物馆和游客中心的室内多楼层导航 demo")
public class IndoorNavigationController {
    private final IndoorNavigationService indoorNavigationService;

    public IndoorNavigationController(IndoorNavigationService indoorNavigationService) {
        this.indoorNavigationService = indoorNavigationService;
    }

    @Operation(summary = "获取室内导航演示建筑")
    @GetMapping("/buildings")
    public List<IndoorBuildingDemo> listBuildings() {
        return indoorNavigationService.listBuildings();
    }

    @Operation(summary = "规划室内导航路线")
    @PostMapping("/plan")
    public IndoorNavigationResponse plan(@RequestBody IndoorNavigationRequest request) {
        return indoorNavigationService.plan(request);
    }
}
