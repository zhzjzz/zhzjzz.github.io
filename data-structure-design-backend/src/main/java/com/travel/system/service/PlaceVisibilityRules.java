package com.travel.system.service;

import java.util.Locale;
import java.util.Set;

public final class PlaceVisibilityRules {

    private static final Set<String> INTERNAL_FACILITY_TYPES = Set.of(
            "教学楼",
            "宿舍",
            "宿舍楼",
            "办公楼",
            "图书馆",
            "核心景点"
    );

    private static final Set<String> INTERNAL_NAME_KEYWORDS = Set.of(
            "教学楼",
            "宿舍",
            "办公楼",
            "图书馆"
    );

    private PlaceVisibilityRules() {
    }

    public static boolean isPublicFacility(String name, String type) {
        return isPublicFacilityType(type) && !containsAny(name, INTERNAL_NAME_KEYWORDS);
    }

    public static boolean isPublicFacilityType(String type) {
        String normalizedType = normalize(type);
        return normalizedType == null || !INTERNAL_FACILITY_TYPES.contains(normalizedType);
    }

    private static boolean containsAny(String value, Set<String> keywords) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return false;
        }
        return keywords.stream().anyMatch(normalizedValue::contains);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
