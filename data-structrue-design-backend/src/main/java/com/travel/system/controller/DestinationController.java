package com.travel.system.controller;

import com.travel.system.model.Destination;
import com.travel.system.repository.DestinationRepository;
import com.travel.system.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目的地控制器。
 * 提供景区/校园的查询、Top-K 推荐与新增能力。
 */
@RestController
@RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationRepository destinationRepository;
    private final RecommendationService recommendationService;

    public DestinationController(DestinationRepository destinationRepository, RecommendationService recommendationService) {
        this.destinationRepository = destinationRepository;
        this.recommendationService = recommendationService;
    }

    /**
     * 目的地列表查询：
     * - keyword 为空：返回全部；
     * - keyword 非空：按名称或类别模糊查询。
     */
    @GetMapping
    public List<Destination> list(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return destinationRepository.findAll();
        }
        return destinationRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
    }

    /**
     * Top-K 推荐接口：按热度+评分综合排序返回前 K 名。
     */
    @GetMapping("/top")
    public List<Destination> top(@RequestParam(defaultValue = "10") int k) {
        return recommendationService.topKDestinations(destinationRepository.findAll(), k);
    }

    /**
     * 新增目的地数据。
     */
    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        return destinationRepository.save(destination);
    }
}
