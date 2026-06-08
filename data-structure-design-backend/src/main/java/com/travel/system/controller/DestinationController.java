package com.travel.system.controller;

import com.travel.system.model.Destination;
import com.travel.system.service.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
@Tag(name = "Destination management", description = "Destination search, recommendation and create APIs")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Operation(summary = "List destinations", description = "Supports keyword search and optional heat/rating sorting")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping
    public List<Destination> list(
            @Parameter(description = "Keyword for fuzzy matching name or category") @RequestParam(required = false) String keyword,
            @Parameter(description = "Sort mode: heat/rating") @RequestParam(required = false) String sort) {
        return destinationService.list(keyword, 1, Integer.MAX_VALUE, sort);
    }

    @Operation(summary = "Route destination search", description = "Search destinations usable by route planning")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping("/route-search")
    public List<Destination> routeSearch(
            @Parameter(description = "Keyword for fuzzy matching") @RequestParam String keyword,
            @Parameter(description = "Maximum result count") @RequestParam(defaultValue = "10") int limit) {
        return destinationService.searchForRoute(keyword, limit);
    }

    @Operation(summary = "Top destinations", description = "Returns Top-K destinations by composite/rating/heat score")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping("/top")
    public List<Destination> top(
            @Parameter(description = "Result count") @RequestParam(defaultValue = "10") int k,
            @Parameter(description = "Ranking mode: composite/rating/heat") @RequestParam(defaultValue = "composite") String mode,
            @Parameter(description = "Interest keyword used as a visible tie breaker") @RequestParam(required = false) String interest) {
        return destinationService.topK(k, mode, interest);
    }

    @Operation(summary = "Create destination", description = "Creates a new destination record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Create succeeded"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        return destinationService.save(destination);
    }
}
