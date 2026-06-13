package com.travel.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.dto.FoodPlaceAnchor;
import com.travel.system.mapper.FoodMapper;
import com.travel.system.model.Food;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AmapFoodSearchService {

    private static final String AROUND_URL = "https://restapi.amap.com/v3/place/around";
    private static final String FOOD_TYPE = "050000";
    private static final int PAGE_SIZE = 25;
    private static final int MAX_LIMIT = 200;
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final Map<String, LatLng> KNOWN_PLACES = Map.ofEntries(
            Map.entry("tiananmen", new LatLng(39.9087, 116.3975)),
            Map.entry("tiananmensquare", new LatLng(39.9042, 116.3975)),
            Map.entry("天安门", new LatLng(39.9087, 116.3975)),
            Map.entry("天安门广场", new LatLng(39.9042, 116.3975)),
            Map.entry("qianmen", new LatLng(39.8990, 116.3979)),
            Map.entry("前门", new LatLng(39.8990, 116.3979)),
            Map.entry("dashilar", new LatLng(39.8936, 116.3926)),
            Map.entry("大栅栏", new LatLng(39.8936, 116.3926)),
            Map.entry("bupt", new LatLng(39.9652, 116.3511)),
            Map.entry("北京邮电大学", new LatLng(39.9652, 116.3511)),
            Map.entry("nanluoguxiang", new LatLng(39.9421, 116.4039)),
            Map.entry("南锣鼓巷", new LatLng(39.9421, 116.4039))
    );

    private final FoodMapper foodMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String configuredKey;
    private final String defaultCity;

    public AmapFoodSearchService(FoodMapper foodMapper,
                                 ObjectMapper objectMapper,
                                 @Value("${amap.web-service-key:${AMAP_KEY:${VITE_AMAP_KEY:}}}") String configuredKey,
                                 @Value("${amap.city:${AMAP_FOOD_CITY:北京}}") String defaultCity) {
        this.foodMapper = foodMapper;
        this.objectMapper = objectMapper;
        this.configuredKey = configuredKey;
        this.defaultCity = defaultCity;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public boolean isConfigured() {
        return apiKey() != null;
    }

    public List<Food> search(String place,
                             String keyword,
                             String cuisine,
                             String sort,
                             int limit,
                             Double radiusMeters,
                             Double minAveragePrice,
                             Double maxAveragePrice) {
        String key = apiKey();
        if (key == null) {
            throw new IllegalStateException("AMAP_KEY is not configured");
        }
        LatLng anchor = resolveAnchor(place);
        if (anchor == null) {
            throw new IllegalArgumentException("无法解析附近地点，请输入已知景点或常用地点");
        }

        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, MAX_LIMIT));
        int safeRadius = (int) Math.max(500, Math.min(radiusMeters == null || radiusMeters <= 0 ? 5_000 : radiusMeters, 50_000));
        List<Food> foods = new ArrayList<>();
        for (int page = 1; foods.size() < safeLimit && page <= 8; page++) {
            String body = fetchAround(key, anchor, keyword, safeRadius, page);
            List<Food> pageFoods;
            try {
                pageFoods = parseAmapPois(body, anchor.lat(), anchor.lng(), safeLimit - foods.size());
            } catch (IOException e) {
                throw new IllegalStateException("解析高德餐饮搜索结果失败", e);
            }
            if (pageFoods.isEmpty()) {
                break;
            }
            foods.addAll(pageFoods);
            if (pageFoods.size() < PAGE_SIZE) {
                break;
            }
        }
        return sortAndFilter(foods, cuisine, sort, minAveragePrice, maxAveragePrice).stream()
                .limit(safeLimit)
                .toList();
    }

    List<Food> parseAmapPois(String body, double anchorLat, double anchorLng, int limit) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!"1".equals(text(root.path("status")))) {
            throw new IllegalStateException("AMap request failed: " + text(root.path("info")));
        }
        List<Food> foods = new ArrayList<>();
        for (JsonNode poi : root.path("pois")) {
            String id = text(poi.path("id"));
            String name = text(poi.path("name"));
            LatLng location = parseLocation(text(poi.path("location")));
            if (id == null || name == null || location == null) {
                continue;
            }
            Food food = new Food();
            food.setId(null);
            food.setName(name);
            food.setStoreName(name);
            food.setCuisine(cuisineFromType(text(poi.path("type"))));
            food.setAddress(text(poi.path("address")));
            food.setLatitude(location.lat());
            food.setLongitude(location.lng());
            food.setSourceType("amap-live");
            food.setSourceId(id);
            food.setImageUrl(firstPhotoUrl(poi.path("photos")));
            food.setRating(number(poi.path("biz_ext").path("rating")));
            food.setAveragePrice(number(poi.path("biz_ext").path("cost")));
            food.setHeat(food.getRating() == null ? 70d : Math.round(food.getRating() * 18));
            Double reportedDistance = number(poi.path("distance"));
            food.setDistanceMeters(reportedDistance == null
                    ? haversineMeters(anchorLat, anchorLng, location.lat(), location.lng())
                    : reportedDistance);
            foods.add(food);
            if (foods.size() >= limit) {
                break;
            }
        }
        return foods;
    }

    private String firstPhotoUrl(JsonNode photos) {
        if (photos == null || !photos.isArray()) {
            return null;
        }
        for (JsonNode photo : photos) {
            String url = normalizeImageUrl(text(photo.path("url")));
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private String normalizeImageUrl(String url) {
        String value = normalize(url);
        if (value == null) {
            return null;
        }
        return value.replaceFirst("(?i)^http://store\\.is\\.autonavi\\.com", "https://store.is.autonavi.com");
    }

    private String fetchAround(String key, LatLng anchor, String keyword, int radius, int page) {
        URI uri = UriComponentsBuilder.fromHttpUrl(AROUND_URL)
                .queryParam("key", key)
                .queryParam("location", anchor.lng() + "," + anchor.lat())
                .queryParam("types", FOOD_TYPE)
                .queryParam("keywords", keyword == null ? "" : keyword)
                .queryParam("city", defaultCity)
                .queryParam("radius", radius)
                .queryParam("offset", PAGE_SIZE)
                .queryParam("page", page)
                .queryParam("extensions", "all")
                .queryParam("sortrule", "distance")
                .queryParam("output", "JSON")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "travel-system-course-project/1.0")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (IOException e) {
            throw new IllegalStateException("调用高德餐饮搜索失败", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用高德餐饮搜索被中断", e);
        }
    }

    private List<Food> sortAndFilter(List<Food> foods,
                                     String cuisine,
                                     String sort,
                                     Double minAveragePrice,
                                     Double maxAveragePrice) {
        Set<String> cuisineKeys = cuisineKeys(cuisine);
        Double min = minAveragePrice == null || minAveragePrice < 0 ? null : minAveragePrice;
        Double max = maxAveragePrice == null || maxAveragePrice < 0 ? null : maxAveragePrice;
        return foods.stream()
                .filter(food -> cuisineKeys.isEmpty() || cuisineKeys(food.getCuisine()).stream().anyMatch(cuisineKeys::contains))
                .filter(food -> food.getAveragePrice() == null
                        || (min == null || food.getAveragePrice() >= min)
                        && (max == null || food.getAveragePrice() <= max))
                .sorted(comparator(sort))
                .toList();
    }

    private Comparator<Food> comparator(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble((Food food) -> safe(food.getRating())).reversed();
        }
        if ("averagePrice".equalsIgnoreCase(sort) || "price".equalsIgnoreCase(sort)) {
            return Comparator.comparingDouble(food -> food.getAveragePrice() == null ? Double.POSITIVE_INFINITY : food.getAveragePrice());
        }
        return Comparator.comparingDouble(food -> food.getDistanceMeters() == null ? Double.POSITIVE_INFINITY : food.getDistanceMeters());
    }

    private LatLng resolveAnchor(String place) {
        String key = placeKey(place);
        if (key == null) {
            return KNOWN_PLACES.get("天安门");
        }
        LatLng known = KNOWN_PLACES.get(key);
        if (known != null) {
            return known;
        }
        List<FoodPlaceAnchor> anchors = foodMapper.findPlaceAnchors();
        if (anchors == null) {
            return null;
        }
        return anchors.stream()
                .filter(anchor -> anchor.getName() != null && anchor.getLatitude() != null && anchor.getLongitude() != null)
                .filter(anchor -> {
                    String anchorKey = placeKey(anchor.getName());
                    return anchorKey != null && (anchorKey.contains(key) || key.contains(anchorKey));
                })
                .findFirst()
                .map(anchor -> new LatLng(anchor.getLatitude(), anchor.getLongitude()))
                .orElse(null);
    }

    private String apiKey() {
        String key = normalize(configuredKey);
        if (key != null) {
            return key;
        }
        return readDevFrontendKey();
    }

    private String readDevFrontendKey() {
        for (Path path : List.of(
                Path.of("../data-structure-design-frontend/.env"),
                Path.of("data-structure-design-frontend/.env"))) {
            if (!Files.exists(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("VITE_AMAP_KEY=")) {
                        return normalize(trimmed.substring("VITE_AMAP_KEY=".length()).replace("\"", "").replace("'", ""));
                    }
                }
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private LatLng parseLocation(String location) {
        if (location == null || !location.contains(",")) {
            return null;
        }
        String[] parts = location.split(",", 2);
        try {
            return new LatLng(Double.parseDouble(parts[1]), Double.parseDouble(parts[0]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String cuisineFromType(String type) {
        String value = normalize(type);
        if (value == null) {
            return "餐饮";
        }
        String detail = value.contains(";") ? value.substring(value.lastIndexOf(';') + 1) : value;
        if (detail.contains("北京菜")) return "京菜";
        if (detail.contains("中餐")) return "中餐";
        if (detail.contains("快餐")) return "快餐";
        if (detail.contains("咖啡")) return "咖啡";
        if (detail.contains("火锅")) return "火锅";
        if (detail.contains("甜品") || detail.contains("冷饮")) return "甜品";
        if (detail.contains("清真")) return "清真菜";
        if (detail.contains("西餐") || detail.contains("外国")) return "西式简餐";
        return detail.isBlank() ? "餐饮" : detail;
    }

    private String cuisineLabel(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        String key = normalized.toLowerCase(Locale.ROOT).replace("-", "_").replaceAll("\\s+", "_");
        return switch (key) {
            case "cafe", "coffee", "coffee_shop", "咖啡馆", "咖啡" -> "咖啡";
            case "fast_food", "fastfood", "burger", "hamburger", "快餐", "西式简餐" -> "快餐";
            case "bakery", "bread", "烘焙", "面包" -> "烘焙";
            case "beijing", "beijing_food", "北京菜", "京菜", "京味小吃" -> "京菜";
            case "chinese", "中餐" -> "中餐";
            case "hotpot", "火锅" -> "火锅";
            case "dessert", "ice_cream", "甜品" -> "甜品";
            case "muslim", "halal", "清真", "清真菜" -> "清真菜";
            default -> normalized;
        };
    }

    private Set<String> cuisineKeys(String value) {
        Set<String> keys = new LinkedHashSet<>();
        addCuisineKey(keys, cuisineLabel(value));
        addCuisineKey(keys, value);

        String compact = placeKey(value);
        if (compact == null) {
            return keys;
        }

        if (containsAny(compact, "beijing", "jingcuisine", "\u4eac\u83dc", "\u5317\u4eac\u83dc", "\u5317\u4eac\u5c0f\u5403", "\u5730\u65b9\u83dc")) {
            addCuisineKeys(keys, "\u4eac\u83dc", "\u5317\u4eac\u83dc", "\u5317\u4eac\u5c0f\u5403", "\u5730\u65b9\u83dc");
        }
        if (containsAny(compact, "chinese", "\u4e2d\u9910", "\u4e2d\u9910\u5385", "\u4e2d\u5f0f")) {
            addCuisineKeys(keys, "\u4e2d\u9910", "\u4e2d\u9910\u5385", "\u4e2d\u5f0f");
        }
        if (containsAny(compact, "coffee", "cafe", "\u5496\u5561", "\u5496\u5561\u5385")) {
            addCuisineKeys(keys, "\u5496\u5561", "\u5496\u5561\u5385", "\u996e\u54c1");
        }
        if (containsAny(compact, "fastfood", "burger", "hamburger", "\u5feb\u9910", "\u5c0f\u5403", "\u6c49\u5821")) {
            addCuisineKeys(keys, "\u5feb\u9910", "\u5c0f\u5403", "\u6c49\u5821");
        }
        if (containsAny(compact, "dessert", "icecream", "\u751c\u54c1", "\u51b7\u996e", "\u51b0\u6dc7\u6dcb")) {
            addCuisineKeys(keys, "\u751c\u54c1", "\u51b7\u996e", "\u51b0\u6dc7\u6dcb", "\u996e\u54c1");
        }
        if (containsAny(compact, "hotpot", "\u706b\u9505")) {
            addCuisineKeys(keys, "\u706b\u9505");
        }
        if (containsAny(compact, "noodle", "\u9762\u98df", "\u9762\u9986")) {
            addCuisineKeys(keys, "\u9762\u98df", "\u9762\u9986");
        }
        if (containsAny(compact, "muslim", "halal", "\u6e05\u771f")) {
            addCuisineKeys(keys, "\u6e05\u771f\u83dc", "\u6e05\u771f");
        }
        if (containsAny(compact, "western", "foreign", "\u897f\u9910", "\u897f\u5f0f", "\u5916\u56fd")) {
            addCuisineKeys(keys, "\u897f\u9910", "\u897f\u5f0f", "\u897f\u5f0f\u7b80\u9910", "\u5916\u56fd\u9910\u5385");
        }
        if (containsAny(compact, "restaurant", "food", "\u9910\u996e", "\u9910\u5385", "\u9910\u9986")) {
            addCuisineKey(keys, "\u9910\u996e");
        }
        return keys;
    }

    private void addCuisineKeys(Set<String> keys, String... values) {
        for (String value : values) {
            addCuisineKey(keys, value);
        }
    }

    private void addCuisineKey(Set<String> keys, String value) {
        String key = placeKey(value);
        if (key != null) {
            keys.add(key);
        }
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isArray() || node.isObject()) {
            return null;
        }
        return normalize(node.asText());
    }

    private Double number(JsonNode node) {
        String value = text(node);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String placeKey(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private record LatLng(double lat, double lng) {
    }
}
