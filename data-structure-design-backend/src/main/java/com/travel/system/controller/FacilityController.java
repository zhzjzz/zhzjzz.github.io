package com.travel.system.controller;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.model.Facility;
import com.travel.system.service.FacilitySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@Tag(name = "Facilities", description = "Facility search APIs")
public class FacilityController {

    private final FacilityMapper facilityMapper;
    private final FacilitySearchService facilitySearchService;

    public FacilityController(FacilityMapper facilityMapper,
                              FacilitySearchService facilitySearchService) {
        this.facilityMapper = facilityMapper;
        this.facilitySearchService = facilitySearchService;
    }

    @Operation(summary = "List facilities", description = "Supports optional facility type fuzzy search")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping
    public List<Facility> list(
            @Parameter(description = "Facility type keyword") @RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) {
            return facilityMapper.findAll().stream()
                    .filter(FacilitySearchService::isVisibleFacility)
                    .toList();
        }
        return facilityMapper.findByFacilityTypeContainingIgnoreCase(type).stream()
                .filter(FacilitySearchService::isVisibleFacility)
                .toList();
    }

    @Operation(summary = "Facility type options", description = "Returns deduped facility types")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping("/types")
    public List<String> types(
            @Parameter(description = "Type keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Destination scene type") @RequestParam(required = false) String sceneType,
            @Parameter(description = "Maximum result count") @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return facilityMapper.findDistinctFacilityTypes(normalizedKeyword, 200).stream()
                .filter(type -> FacilitySearchService.isVisibleFacilityTypeForScene(type, sceneType))
                .limit(safeLimit)
                .toList();
    }

    @Operation(summary = "Nearby facilities", description = "Search nearby facilities by straight or road-network distance")
    @ApiResponse(responseCode = "200", description = "Search succeeded")
    @GetMapping("/nearby")
    public List<FacilityQueryResult> nearby(
            @Parameter(description = "Start latitude") @RequestParam Double fromLat,
            @Parameter(description = "Start longitude") @RequestParam Double fromLon,
            @Parameter(description = "Start road node ID") @RequestParam(required = false) Long fromNodeId,
            @Parameter(description = "Facility type filter") @RequestParam(required = false) String type,
            @Parameter(description = "Name or description keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Maximum search distance in meters") @RequestParam(required = false) Double maxDistanceMeters,
            @Parameter(description = "Destination or spot name") @RequestParam(required = false) String spotName,
            @Parameter(description = "Destination scene type") @RequestParam(required = false) String sceneType) {
        return facilitySearchService.searchNearby(fromLat, fromLon, fromNodeId, type, keyword, maxDistanceMeters, spotName, sceneType);
    }
}
