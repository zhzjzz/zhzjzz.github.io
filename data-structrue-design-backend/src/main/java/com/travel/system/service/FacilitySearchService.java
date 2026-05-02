package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.model.Facility;
import com.travel.system.mapper.FacilityMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class FacilitySearchService {
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final FacilityMapper facilityMapper;

    public FacilitySearchService(FacilityMapper facilityMapper) {
        this.facilityMapper = facilityMapper;
    }

    public List<FacilityQueryResult> searchNearby(Double fromLat,
                                                  Double fromLon,
                                                  String facilityType,
                                                  String keyword,
                                                  Double maxDistanceMeters) {
        List<Facility> facilities = facilityMapper.findAll();

        return facilities.stream()
                .filter(facility -> matchesFacilityType(facility, facilityType))
                .filter(facility -> matchesKeyword(facility, keyword))
                .map(facility -> toResult(facility, fromLat, fromLon))
                .filter(Objects::nonNull)
                .filter(result -> maxDistanceMeters == null || result.getDistanceMeters() <= maxDistanceMeters)
                .sorted(Comparator.comparingDouble(FacilityQueryResult::getDistanceMeters))
                .toList();
    }

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

    private boolean matchesFacilityType(Facility facility, String facilityType) {
        if (facilityType == null || facilityType.isBlank()) {
            return true;
        }
        return containsIgnoreCase(facility.getFacilityType(), facilityType);
    }

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

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private record LatLng(double lat, double lon) {
    }
}
