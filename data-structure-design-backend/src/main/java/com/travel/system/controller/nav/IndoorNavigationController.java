package com.travel.system.controller.nav;

import com.travel.system.dto.IndoorNavigationRequest;
import com.travel.system.dto.IndoorNavigationResponse;
import com.travel.system.service.nav.IndoorNavigationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nav/indoor")
public class IndoorNavigationController {

    private final IndoorNavigationService service;

    public IndoorNavigationController(IndoorNavigationService service) {
        this.service = service;
    }

    @PostMapping("/plan")
    public IndoorNavigationResponse plan(@RequestBody IndoorNavigationRequest request) {
        return service.plan(request.getBuildingName(), request.getFrom(), request.getTo());
    }
}
