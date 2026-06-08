package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.mapper.DestinationMapper;
import com.travel.system.model.Destination;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DestinationService {

    private static final Set<String> BLOCKED_DESTINATION_NAMES = Set.of("北京吉利学院(旧址)");

    private final DestinationMapper destinationMapper;
    private final RecommendationService recommendationService;

    public DestinationService(DestinationMapper destinationMapper,
                              RecommendationService recommendationService) {
        this.destinationMapper = destinationMapper;
        this.recommendationService = recommendationService;
    }

    /**

     * 按查询条件读取列表数据；分页、过滤或排序规则由 service 层统一处理。

     */
    public List<Destination> list(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return visibleDestinations(destinationMapper.findAll());
        }
        return visibleDestinations(destinationMapper.findByKeyword(keyword));
    }

    public List<Destination> list(String keyword, int page, int size, String sort) {
        return sortDestinations(list(keyword, page, size), sort);
    }

    public Destination findById(Long id) {
        if (id == null) {
            return null;
        }
        Destination destination = destinationMapper.findById(id);
        return isVisibleDestination(destination) ? destination : null;
    }

    public List<Destination> findAll() {
        return visibleDestinations(destinationMapper.findAll());
    }

    /**

     * 按默认或指定推荐策略返回前 k 条数据，k 非法时由 service 内部修正为安全默认值。

     */
    public List<Destination> topK(int k) {
        return topK(k, "composite");
    }

    /**

     * 按默认或指定推荐策略返回前 k 条数据，k 非法时由 service 内部修正为安全默认值。

     */
    public List<Destination> topK(int k, String mode) {
        List<Destination> all = visibleDestinations(destinationMapper.findAll());
        int safeK = Math.max(1, Math.min(k, 50));
        return recommendationService.topKDestinations(all, safeK, parseRankingMode(mode));
    }

    public List<Destination> topK(int k, String mode, String interest) {
        List<Destination> all = visibleDestinations(destinationMapper.findAll());
        int safeK = Math.max(1, Math.min(k, 50));
        return recommendationService.topKDestinations(all, safeK, parseRankingMode(mode), interest);
    }
    /**
     * 将前端传入的推荐模式字符串转换为枚举值；不认识的值统一回退到综合排序，避免接口报错。
     */

    private RecommendationService.DestinationRankingMode parseRankingMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return RecommendationService.DestinationRankingMode.COMPOSITE;
        }
        return switch (mode.trim().toLowerCase()) {
            case "rating" -> RecommendationService.DestinationRankingMode.RATING;
            case "heat" -> RecommendationService.DestinationRankingMode.HEAT;
            default -> RecommendationService.DestinationRankingMode.COMPOSITE;
        };
    }

    private List<Destination> sortDestinations(List<Destination> data, String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return data.stream()
                    .sorted((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating())))
                    .toList();
        }
        if ("heat".equalsIgnoreCase(sort)) {
            return data.stream()
                    .sorted((a, b) -> Double.compare(safe(b.getHeat()), safe(a.getHeat())))
                    .toList();
        }
        return data;
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    /**

     * 按关键词搜索可用于路线规划的目的地，并限制返回数量，供前端地点选择器使用。

     */
    public List<Destination> searchForRoute(String keyword, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return destinationMapper.findByKeyword(normalizedKeyword)
                .stream()
                .filter(this::isVisibleDestination)
                .filter(d -> d.getLatitude() != null && d.getLongitude() != null)
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    /**

     * 保存或更新实体数据，并返回数据库持久化后的结果。

     */
    public Destination save(Destination destination) {
        destinationMapper.save(destination);
        return destination;
    }

    private List<Destination> visibleDestinations(List<Destination> destinations) {
        return destinations.stream()
                .filter(this::isVisibleDestination)
                .toList();
    }

    private boolean isVisibleDestination(Destination destination) {
        return destination != null && !BLOCKED_DESTINATION_NAMES.contains(destination.getName());
    }
}
