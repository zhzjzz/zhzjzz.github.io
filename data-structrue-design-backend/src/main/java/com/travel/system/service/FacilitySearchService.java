package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.model.RoadNode;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.repository.FacilitySearchRepository;
import com.travel.system.mapper.RoadNodeMapper;
import com.travel.system.search.FacilityDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 设施搜索服务类
 * <p>
 * 提供基于位置的路网距离设施搜索功能，主要特性：
 * <ul>
 *     <li>支持设施类型过滤（如咖啡馆、食堂、卫生间等）</li>
 *     <li>支持关键字搜索（名称、类型、所在目的地）</li>
 *     <li>支持最大距离限制筛选</li>
 *     <li>基于路网计算实际步行/骑行距离（非直线距离）</li>
 *     <li>结果按距离升序排序</li>
 * </ul>
 * </p>
 * <p>
 * 搜索策略：优先使用 Elasticsearch 进行全文模糊搜索，
 * ES 不可用时回退到 MySQL 全量查询
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see Facility 设施实体类
 * @see FacilityQueryResult 设施查询结果DTO
 */
@Service
public class FacilitySearchService {

    /**
     * 设施数据访问映射器（MySQL）
     */
    private final FacilityMapper facilityRepository;
    
    /**
     * 设施搜索仓库（Elasticsearch），可为 null
     */
    private final FacilitySearchRepository facilitySearchRepository;
    
    /**
     * 道路节点数据访问映射器
     */
    private final RoadNodeMapper roadNodeMapper;
    
    /**
     * 路线规划服务，用于计算路网最短距离
     */
    private final RoutePlanningService routePlanningService;

    /**
     * 构造函数，注入依赖
     *
     * @param facilityRepository 设施数据访问映射器
     * @param facilitySearchRepositoryProvider ES 搜索仓库提供者（可选）
     * @param roadNodeRepository 道路节点数据访问映射器
     * @param routePlanningService 路线规划服务
     */
    public FacilitySearchService(FacilityMapper facilityRepository,
                                 ObjectProvider<FacilitySearchRepository> facilitySearchRepositoryProvider,
                                 RoadNodeMapper roadNodeRepository,
                                 RoutePlanningService routePlanningService) {
        this.facilityRepository = facilityRepository;
        this.facilitySearchRepository = facilitySearchRepositoryProvider.getIfAvailable();
        this.roadNodeMapper = roadNodeRepository;
        this.routePlanningService = routePlanningService;
    }

    /**
     * 搜索附近设施
     * <p>
     * 综合搜索流程：
     * <ol>
     *     <li>获取候选设施列表（ES优先，MySQL回退）</li>
     *     <li>获取所有道路节点作为路网入口</li>
     *     <li>使用 Dijkstra 计算从起点到所有节点的最短路网距离</li>
     *     <li>为每个设施找到最近的道路节点，并获取路网距离</li>
     *     <li>应用过滤条件：设施类型、关键字、最大距离</li>
     *     <li>按路网距离升序排序返回</li>
     * </ol>
     * </p>
     *
     * @param fromNodeId        起点道路节点ID（用户当前位置对应的最近路网节点）
     * @param facilityType      可选：设施类型过滤条件（如"咖啡馆"、"食堂"）
     * @param keyword           可选：关键字搜索（匹配名称、类型、目的地）
     * @param maxDistanceMeters 可选：最大距离限制（米）
     * @param transport         交通方式（如"walk"、"bike"），影响路网计算
     * @return 设施查询结果列表，包含设施信息和路网距离；按距离升序排列
     */
    public List<FacilityQueryResult> searchNearby(Long fromNodeId,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters,
                                                  String transport) {
        // 优先获取设施列表（使用 ES 模糊搜索或全量查询）
        List<Facility> facilities = searchFacilities(facilityType, keyword);
        
        // 获取道路网络节点和最短距离映射
        List<RoadNode> roadNodes = roadNodeMapper.findAll();
        Map<Long, Double> distanceMap = routePlanningService.shortestDistanceMap(fromNodeId, transport);

        return facilities.stream()
                .filter(facility -> matchesFacilityType(facility, facilityType))
                .filter(facility -> matchesKeyword(facility, keyword))
                .map(facility -> toResult(facility, roadNodes, distanceMap))
                .filter(Objects::nonNull)
                .filter(result -> maxDistanceMeters == null || result.getRouteDistanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(FacilityQueryResult::getRouteDistanceMeters))
                .toList();
    }
    
    /**
     * 搜索设施列表，优先使用 Elasticsearch 进行模糊搜索
     * <p>
     * 搜索策略：
     * <ul>
     *     <li>有关键字时：使用 ES 全文搜索名称、类型、目的地</li>
     *     <li>无关键字但有类型时：使用 ES 按类型过滤</li>
     *     <li>ES 不可用时：回退到 MySQL 全量查询</li>
     * </ul>
     * </p>
     *
     * @param facilityType 设施类型过滤条件
     * @param keyword      搜索关键字
     * @return 设施实体列表
     */
    private List<Facility> searchFacilities(String facilityType, String keyword) {
        // 如果有搜索条件，优先使用 ES
        if ((keyword != null && !keyword.isBlank()) || (facilityType != null && !facilityType.isBlank())) {
            if (facilitySearchRepository != null) {
                try {
                    List<FacilityDocument> docs;
                    if (keyword != null && !keyword.isBlank()) {
                        // 使用关键字搜索名称、类型或目的地
                        docs = facilitySearchRepository
                            .findByNameContainingOrFacilityTypeContainingOrDestinationNameContaining(
                                keyword, keyword, keyword);
                    } else {
                        // 只按类型搜索
                        docs = StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(
                                    facilitySearchRepository.findAll().iterator(), 0), false)
                            .filter(doc -> doc.getFacilityType() != null 
                                && doc.getFacilityType().toLowerCase(Locale.ROOT)
                                    .contains(facilityType.toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
                    }
                    return docs.stream().map(this::toFacility).collect(Collectors.toList());
                } catch (Exception e) {
                    // ES 搜索失败时回退到 MySQL
                }
            }
        }
        
        // 使用 MySQL 全量查询
        return facilityRepository.findAll();
    }

    /**
     * 将设施实体转换为查询结果对象
     * <p>
     * 计算设施到起点的路网距离：
     * <ol>
     *     <li>找到设施的最近道路节点</li>
     *     <li>从距离映射表中获取到该节点的路网距离</li>
     *     <li>封装为查询结果</li>
     * </ol>
     * </p>
     *
     * @param facility    设施实体
     * @param roadNodes   所有道路节点
     * @param distanceMap 节点ID到最短距离的映射
     * @return 设施查询结果；若无法计算距离则返回 null
     */
    private FacilityQueryResult toResult(Facility facility,
                                         List<RoadNode> roadNodes,
                                         Map<Long, Double> distanceMap) {
        // 找到设施附近的最近路网节点
        RoadNode nearestNode = findNearestNode(facility, roadNodes);
        if (nearestNode == null) {
            return null;
        }
        // 获取从起点到该节点的路网距离
        Double routeDistance = distanceMap.get(nearestNode.getId());
        if (routeDistance == null || routeDistance.isInfinite()) {
            return null;
        }
        return new FacilityQueryResult(facility, nearestNode.getId(), nearestNode.getName(), routeDistance);
    }

    /**
     * 检查设施类型是否匹配
     *
     * @param facility     设施实体
     * @param facilityType 目标设施类型
     * @return true 如果匹配或没有指定类型
     */
    private boolean matchesFacilityType(Facility facility, String facilityType) {
        if (facilityType == null || facilityType.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getFacilityType(), facilityType);
    }

    /**
     * 检查设施是否匹配关键字
     * <p>
     * 在设施名称、类型、所属目的地名称中搜索关键字
     * </p>
     *
     * @param facility 设施实体
     * @param keyword  搜索关键字
     * @return true 如果匹配或没有指定关键字
     */
    private boolean matchesKeyword(Facility facility, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getName(), keyword)
                || containsIgnoreCase(facility.getFacilityType(), keyword)
                || (facility.getDestination() != null && containsIgnoreCase(facility.getDestination().getName(), keyword));
    }

    /**
     * 查找设施最近的道路节点
     * <p>
     * 使用欧几里得距离（简化计算）找到设施坐标最近的节点。
     * 如果设施自身没有坐标，尝试使用其所属目的地的坐标
     * </p>
     *
     * @param facility  设施实体
     * @param roadNodes 候选道路节点列表
     * @return 最近的道路节点；若无法计算则返回 null
     */
    private RoadNode findNearestNode(Facility facility, List<RoadNode> roadNodes) {
        // 筛选有坐标信息的节点
        List<RoadNode> candidates = roadNodes.stream()
                .filter(node -> node.getLatitude() != null && node.getLongitude() != null)
                .toList();

        // 获取设施坐标（优先使用设施自身坐标，否则使用目的地坐标）
        Double lat = facility.getLatitude();
        Double lng = facility.getLongitude();

        if (lat == null || lng == null) {
            if (facility.getDestination() != null) {
                lat = facility.getDestination().getLatitude();
                lng = facility.getDestination().getLongitude();
            }
        }

        // 计算最近节点
        if (lat != null && lng != null && !candidates.isEmpty()) {
            final double targetLat = lat;
            final double targetLng = lng;
            return candidates.stream()
                    .min(Comparator.comparingDouble(node -> squaredDistance(node, targetLat, targetLng)))
                    .orElse(null);
        }

        // 无效时返回第一个节点
        if (!roadNodes.isEmpty()) {
            return roadNodes.get(0);
        }
        return null;
    }

    /**
     * 计算平方距离（简化版，避免开方运算）
     * <p>
     * 用于比较距离大小，实际距离 = sqrt(squaredDistance)
     * </p>
     *
     * @param node    道路节点
     * @param lat     目标纬度
     * @param lng     目标经度
     * @return 平方距离值
     */
    private double squaredDistance(RoadNode node, double lat, double lng) {
        double dLat = node.getLatitude() - lat;
        double dLng = node.getLongitude() - lng;
        return dLat * dLat + dLng * dLng;
    }

    /**
     * 不区分大小写检查字符串是否包含子串
     *
     * @param source 来源字符串
     * @param keyword 要查找的关键字
     * @return true 如果包含（不区分大小写）
     */
    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
    
    /**
     * FacilityDocument 转换为 Facility
     * <p>
     * 将 Elasticsearch 文档转换为实体对象
     * </p>
     *
     * @param doc ES 设施文档
     * @return 设施实体对象
     */
    private Facility toFacility(FacilityDocument doc) {
        Facility facility = new Facility();
        facility.setId(Long.valueOf(doc.getId()));
        facility.setName(doc.getName());
        facility.setFacilityType(doc.getFacilityType());
        facility.setLatitude(doc.getLatitude());
        facility.setLongitude(doc.getLongitude());
        return facility;
    }
}
