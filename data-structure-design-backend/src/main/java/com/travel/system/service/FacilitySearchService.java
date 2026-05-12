package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.mapper.FacilityMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class FacilitySearchService {
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final Set<String> SCENIC_NON_SERVICE_TYPES = Set.of("教学楼", "办公楼", "宿舍楼", "核心景点", "食堂", "图书馆");

    private final FacilityMapper facilityMapper;

    public FacilitySearchService(FacilityMapper facilityMapper) {
        this.facilityMapper = facilityMapper;
    }
    /**
     * 按用户当前位置搜索附近设施。fromLat/fromLon 是当前位置，facilityType 和 keyword 是过滤条件，maxDistanceMeters 是最大距离限制；返回结果按距离升序排列。
     */

    public List<FacilityQueryResult> searchNearby(Double fromLat,
                                                  Double fromLon,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters) {
        return searchNearby(fromLat, fromLon, facilityType, keyword, maxDistanceMeters, null, null);
    }

    public List<FacilityQueryResult> searchNearby(Double fromLat,
                                                  Double fromLon,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters,
                                                  String spotName,
                                                  String sceneType) {
        List<Facility> facilities = facilityMapper.findAll();

        return facilities.stream()
                .filter(facility -> matchesSpotName(facility, spotName))
                .filter(facility -> matchesSceneType(facility, sceneType))
                .filter(facility -> matchesFacilityType(facility, facilityType))
                .filter(facility -> matchesKeyword(facility, keyword))
                .map(facility -> toResult(facility, fromLat, fromLon))
                .filter(Objects::nonNull)
                .filter(result -> maxDistanceMeters == null || result.getDistanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(FacilityQueryResult::getDistanceMeters))
                .toList();
    }

    public static boolean isVisibleFacilityTypeForScene(String facilityType, String sceneType) {
        if (!isScenicScene(sceneType)) {
            return true;
        }
        return facilityType != null && !SCENIC_NON_SERVICE_TYPES.contains(facilityType);
    }

    private boolean matchesSpotName(Facility facility, String spotName) {
        if (spotName == null || spotName.isBlank()) {
            return true;
        }
        return facility.getDestination() != null
                && spotName.trim().equals(facility.getDestination().getName());
    }

    private boolean matchesSceneType(Facility facility, String sceneType) {
        if (!isScenicScene(sceneType)) {
            return true;
        }
        return "poi".equalsIgnoreCase(facility.getSourceType())
                && isVisibleFacilityTypeForScene(facility.getFacilityType(), sceneType);
    }

    private static boolean isScenicScene(String sceneType) {
        return "景区".equals(sceneType) || "景点".equals(sceneType);
    }

    /**

     * 将 Facility 实体转换为前端查询结果 DTO，并在传入用户位置时计算距离。

     */
    private FacilityQueryResult toResult(Facility facility, Double fromLat, Double fromLon) {
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

     * 判断设施类型是否满足筛选条件；筛选条件为空时视为全部匹配。

     */
    private boolean matchesFacilityType(Facility facility, String facilityType) {
        if (facilityType == null || facilityType.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getFacilityType(), facilityType);
    }

    /**

     * 判断设施名称、类型或所属目的地等文本字段是否包含查询关键词。

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

     * 解析设施坐标；当设施自身坐标缺失时尝试使用所属目的地坐标兜底。

     */
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

    /**

     * 使用 Haversine 公式计算两个经纬度点之间的球面距离，结果单位为米。

     */
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

     * 执行忽略大小写的包含判断，并安全处理空字符串。

     */
    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /**

     * 保存经纬度坐标的轻量值对象，用于距离计算和位置兜底。

     */
    private record LatLng(double lat, double lon) {
    }
}
