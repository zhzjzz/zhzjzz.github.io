package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.mapper.DestinationMapper;
import com.travel.system.model.Destination;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DestinationService {

    private final DestinationMapper destinationMapper;
    private final RecommendationService recommendationService;

    public DestinationService(DestinationMapper destinationMapper,
                              RecommendationService recommendationService) {
        this.destinationMapper = destinationMapper;
        this.recommendationService = recommendationService;
    }

    public List<Destination> list(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return destinationMapper.findAll();
        }
        return destinationMapper.findByKeyword(keyword);
    }

    public List<Destination> topK(int k) {
        List<Destination> all = destinationMapper.findAll();
        return recommendationService.topKDestinations(all, k);
    }

    public List<Destination> searchForRoute(String keyword, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return destinationMapper.findByKeyword(normalizedKeyword)
                .stream()
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    public Destination save(Destination destination) {
        destinationMapper.save(destination);
        return destination;
    }
}
