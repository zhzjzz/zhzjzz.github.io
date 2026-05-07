package com.travel.system.controller.nav;

import com.travel.system.dto.SpotSearchRequest;
import com.travel.system.model.nav.Spot;
import com.travel.system.service.nav.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nav/spots")
@Tag(name = "景点/高校查询", description = "基于 SQLite 的景区与高校搜索接口")
public class SpotController {

    private final SpotService spotService;

    public SpotController(SpotService spotService) {
        this.spotService = spotService;
    }

    @Operation(summary = "搜索景点/高校")
    @GetMapping
    public List<Spot> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return spotService.search(keyword, category, sortBy, limit);
    }

    @Operation(summary = "获取景点/高校详情")
    @GetMapping("/{spotId}")
    public Spot getById(@PathVariable Long spotId) {
        return spotService.getById(spotId);
    }

    @Operation(summary = "获取全部景点/高校")
    @GetMapping("/all")
    public List<Spot> listAll() {
        return spotService.listAll();
    }
}
