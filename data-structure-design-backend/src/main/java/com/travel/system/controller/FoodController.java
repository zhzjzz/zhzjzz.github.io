package com.travel.system.controller;

import com.travel.system.model.Food;
import com.travel.system.service.FoodService;
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
@RequestMapping("/api/foods")
@Tag(name = "Food", description = "Food search and recommendations")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @Operation(summary = "Search foods", description = "Search by food, shop type, destination, nearby place, radius and sort order")
    @ApiResponse(responseCode = "200", description = "Search succeeded")
    @GetMapping
    public List<Food> search(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Cuisine or shop type") @RequestParam(required = false) String cuisine,
            @Parameter(description = "Destination ID") @RequestParam(required = false) Long destinationId,
            @Parameter(description = "Sort: recommend/rating/destinationHeat/distance") @RequestParam(defaultValue = "recommend") String sort,
            @Parameter(description = "Result limit") @RequestParam(defaultValue = "30") int limit,
            @Parameter(description = "Nearby place or landmark") @RequestParam(required = false) String place,
            @Parameter(description = "Center latitude") @RequestParam(required = false) Double latitude,
            @Parameter(description = "Center longitude") @RequestParam(required = false) Double longitude,
            @Parameter(description = "Search radius in meters") @RequestParam(required = false) Double radiusMeters,
            @Parameter(description = "Minimum average price per person") @RequestParam(required = false) Double minAveragePrice,
            @Parameter(description = "Maximum average price per person") @RequestParam(required = false) Double maxAveragePrice) {
        return foodService.search(keyword, cuisine, destinationId, sort, limit, place, latitude, longitude,
                radiusMeters, minAveragePrice, maxAveragePrice);
    }

    @Operation(summary = "Food cuisine list", description = "Return all available food cuisines")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping("/cuisines")
    public List<String> cuisines() {
        return foodService.cuisines();
    }

    @Operation(summary = "Top foods", description = "Return top-k foods by recommendation score")
    @ApiResponse(responseCode = "200", description = "Query succeeded")
    @GetMapping("/top")
    public List<Food> top(
            @Parameter(description = "Result count, default 10") @RequestParam(defaultValue = "10") int k) {
        return foodService.topK(k);
    }
}
