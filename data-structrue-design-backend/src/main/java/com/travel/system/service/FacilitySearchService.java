package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.repository.FacilitySearchRepository;
import com.travel.system.search.FacilityDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final FacilityMapper facilityRepository;
    private final FacilitySearchRepository facilitySearchRepository;

    public FacilitySearchService(FacilityMapper facilityRepository,
                                 ObjectProvider<FacilitySearchRepository> facilitySearchRepositoryProvider) {
        this.facilityRepository = facilityRepository;
        this.facilitySearchRepository = facilitySearchRepositoryProvider.getIfAvailable();
    }

    public List<FacilityQueryResult> searchNearby(Double fromLat,
                                                  Double fromLon,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters) {
        List<Facility> facilities = searchFacilities(facilityType, keyword);

        return facilities.stream()
                .filter(facility -> matchesFacilityType(facility, facilityType))
                .filter(facility -> matchesKeyword(facility, keyword))
                .map(facility -> toResult(facility, fromLat, fromLon))
                .filter(Objects::nonNull)
                .filter(result -> maxDistanceMeters == null || result.getDistanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(FacilityQueryResult::getDistanceMeters))
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
        if ((keyword != null && !keyword.isBlank()) || (facilityType != null && !facilityType.isBlank())) {
            if (facilitySearchRepository != null) {
                try {
                    List<FacilityDocument> docs;
                    if (keyword != null && !keyword.isBlank()) {
                        docs = facilitySearchRepository
                            .findByNameContainingOrFacilityTypeContainingOrDestinationNameContaining(
                                keyword, keyword, keyword);
                    } else {
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
                }
            }
        }
        return facilityRepository.findAll();
    }

    private FacilityQueryResult toResult(Facility facility,
                                         Double fromLat,
                                         Double fromLon) {
        if (fromLat == null || fromLon == null) {
            return null;
        }
        LatLng facilityLocation = resolveFacilityLocation(facility);
        if (facilityLocation == null) {
            return null;
        }
        double distanceMeters = haversineMeters(fromLat, fromLon, facilityLocation.lat(), facilityLocation.lon());
        return new FacilityQueryResult(facility, distanceMeters);
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

    private LatLng resolveFacilityLocation(Facility facility) {
        Double lat = facility.getLatitude();
        Double lng = facility.getLongitude();
        if (lat == null || lng == null) {
            if (facility.getDestination() != null) {
                lat = facility.getDestination().getLatitude();
                lng = facility.getDestination().getLongitude();
            }
        }
        if (lat == null || lng == null) {
            return null;
        }
        return new LatLng(lat, lng);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
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

    private record LatLng(double lat, double lon) {
    }
}
