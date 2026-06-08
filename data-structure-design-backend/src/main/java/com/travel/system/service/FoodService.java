package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.dto.FoodPlaceAnchor;
import com.travel.system.model.Food;
import com.travel.system.mapper.FoodMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FoodService {

    private static final double DEFAULT_NEARBY_RADIUS_METERS = 3_000d;
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final List<String> CUISINE_ORDER = List.of(
            "咖啡", "快餐", "烘焙", "京菜", "中餐", "面食", "火锅", "甜品", "饮品", "素食", "清真菜", "餐饮"
    );
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

    private final FoodMapper foodRepository;
    private final RecommendationService recommendationService;

    public FoodService(FoodMapper foodRepository,
                       RecommendationService recommendationService) {
        this.foodRepository = foodRepository;
        this.recommendationService = recommendationService;
    }

    /**

     * 按关键词、分类、排序字段和数量限制查询景区数据，供景区检索和推荐页面使用。

     */
    public List<Food> search(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return foodRepository.findAll();
        }
        return foodRepository.findByKeyword(keyword);
    }

    public List<Food> search(String keyword, String cuisine, Long destinationId, String sort, int limit) {
        return search(keyword, cuisine, destinationId, sort, limit, null, null, null, null);
    }

    public List<Food> search(String keyword,
                             String cuisine,
                             Long destinationId,
                             String sort,
                             int limit,
                             String place,
                             Double latitude,
                             Double longitude,
                             Double radiusMeters) {
        return search(keyword, cuisine, destinationId, sort, limit, place, latitude, longitude, radiusMeters, null, null);
    }

    public List<Food> search(String keyword,
                             String cuisine,
                             Long destinationId,
                             String sort,
                             int limit,
                             String place,
                             Double latitude,
                             Double longitude,
                             Double radiusMeters,
                             Double minAveragePrice,
                             Double maxAveragePrice) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCuisine = normalizeCuisineFilter(cuisine);
        int safeLimit = limit <= 0 ? 30 : Math.min(limit, 100);
        List<Food> allFoods = foodRepository.findAll();
        SearchIntent intent = resolveSearchIntent(normalizedKeyword, place, latitude, longitude, allFoods);
        double safeRadiusMeters = radiusMeters == null || radiusMeters <= 0 ? DEFAULT_NEARBY_RADIUS_METERS : radiusMeters;
        PriceRange priceRange = normalizePriceRange(minAveragePrice, maxAveragePrice);

        List<Food> filtered = allFoods.stream()
                .map(food -> withDistance(food, intent.anchor()))
                .filter(food -> matchesKeyword(food, intent.foodKeyword()))
                .filter(food -> matchesCuisine(food, normalizedCuisine))
                .filter(food -> destinationId == null || (food.getDestination() != null && destinationId.equals(food.getDestination().getId())))
                .filter(food -> intent.anchor() == null || food.getDistanceMeters() != null && food.getDistanceMeters() <= safeRadiusMeters)
                .filter(food -> matchesAveragePrice(food, priceRange))
                .toList();

        return sortFoods(filtered, sort, intent.anchor() != null).stream().limit(safeLimit).toList();
    }

    public List<String> cuisines() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String cuisine : foodRepository.findCuisines()) {
            String label = cuisineLabel(cuisine);
            if (label != null) {
                labels.putIfAbsent(placeKey(label), label);
            }
        }
        return labels.values().stream()
                .sorted(Comparator.comparingInt(this::cuisineSortIndex).thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    public List<FoodPlaceAnchor> placeAnchors() {
        Map<String, FoodPlaceAnchor> anchors = new LinkedHashMap<>();
        List<FoodPlaceAnchor> rows = foodRepository.findPlaceAnchors();
        if (rows == null) {
            return List.of();
        }
        for (FoodPlaceAnchor row : rows) {
            if (row.getName() == null || row.getName().isBlank()
                    || row.getLatitude() == null || row.getLongitude() == null) {
                continue;
            }
            String key = placeKey(row.getName());
            anchors.putIfAbsent(key, row);
        }
        return anchors.values().stream()
                .sorted(Comparator.comparing(FoodPlaceAnchor::getName))
                .toList();
    }

    /**

     * 按默认或指定推荐策略返回前 k 条数据，k 非法时由 service 内部修正为安全默认值。

     */
    public List<Food> topK(int k) {
        List<Food> all = foodRepository.findAll();
        return recommendationService.topKFood(all, k);
    }

    /**

     * 保存或更新实体数据，并返回数据库持久化后的结果。

     */
    public Food save(Food food) {
        return foodRepository.save(food);
    }

    private boolean matchesKeyword(Food food, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(food.getName(), keyword)
                || contains(food.getCuisine(), keyword)
                || contains(food.getStoreName(), keyword)
                || (food.getDestination() != null && contains(food.getDestination().getName(), keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean matchesCuisine(Food food, String normalizedCuisine) {
        if (normalizedCuisine == null) {
            return true;
        }
        return normalizedCuisine.equals(normalizeCuisineFilter(food.getCuisine()));
    }

    private boolean matchesAveragePrice(Food food, PriceRange priceRange) {
        if (priceRange == null) {
            return true;
        }
        Double price = food.getAveragePrice();
        if (price == null) {
            return false;
        }
        return (priceRange.min() == null || price >= priceRange.min())
                && (priceRange.max() == null || price <= priceRange.max());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCuisineFilter(String value) {
        String label = cuisineLabel(value);
        return label == null ? null : placeKey(label);
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
            case "beijing", "jing_cuisine", "京菜", "京味小吃" -> "京菜";
            case "chinese", "中餐" -> "中餐";
            case "noodle", "noodles", "面食" -> "面食";
            case "hotpot", "火锅" -> "火锅";
            case "dessert", "ice_cream", "甜品" -> "甜品";
            case "tea", "beverages", "drink", "drinks", "饮品" -> "饮品";
            case "vegetarian", "素食" -> "素食";
            case "muslim", "halal", "清真", "清真菜" -> "清真菜";
            case "unknown", "restaurant", "restaurants", "food", "foods", "餐厅", "餐馆", "餐饮" -> "餐饮";
            default -> normalized;
        };
    }

    private int cuisineSortIndex(String value) {
        int index = CUISINE_ORDER.indexOf(value);
        return index < 0 ? CUISINE_ORDER.size() : index;
    }

    private PriceRange normalizePriceRange(Double minAveragePrice, Double maxAveragePrice) {
        Double min = minAveragePrice == null || minAveragePrice < 0 ? null : minAveragePrice;
        Double max = maxAveragePrice == null || maxAveragePrice < 0 ? null : maxAveragePrice;
        if (min != null && max != null && min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        return min == null && max == null ? null : new PriceRange(min, max);
    }

    private List<Food> sortFoods(List<Food> foods, String sort, boolean hasAnchor) {
        if ("averagePrice".equalsIgnoreCase(sort) || "price".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted(Comparator
                            .comparingDouble((Food food) -> food.getAveragePrice() == null
                                    ? Double.POSITIVE_INFINITY
                                    : food.getAveragePrice())
                            .thenComparing((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating()))))
                    .toList();
        }
        if ("rating".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating())))
                    .toList();
        }
        if ("heat".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted((a, b) -> Double.compare(safe(b.getHeat()), safe(a.getHeat())))
                    .toList();
        }
        if ("distance".equalsIgnoreCase(sort) || hasAnchor && (sort == null || "recommend".equalsIgnoreCase(sort))) {
            return foods.stream()
                    .sorted(Comparator
                            .comparingDouble((Food food) -> food.getDistanceMeters() == null
                                    ? Double.POSITIVE_INFINITY
                                    : food.getDistanceMeters())
                            .thenComparing((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating()))))
                    .toList();
        }
        if ("destinationHeat".equalsIgnoreCase(sort)) {
            return foods.stream()
                    .sorted((a, b) -> Double.compare(destinationHeat(b), destinationHeat(a)))
                    .toList();
        }
        return recommendationService.topKFood(foods, Math.min(10, foods.size()));
    }

    private SearchIntent resolveSearchIntent(String keyword,
                                             String place,
                                             Double latitude,
                                             Double longitude,
                                             List<Food> foods) {
        if (latitude != null && longitude != null) {
            return new SearchIntent(keyword, new LatLng(latitude, longitude));
        }

        String explicitPlace = normalize(place);
        if (explicitPlace != null) {
            return new SearchIntent(keyword, resolvePlace(explicitPlace, foods));
        }

        NearbyPhrase nearbyPhrase = parseNearbyPhrase(keyword);
        if (nearbyPhrase != null) {
            return new SearchIntent(nearbyPhrase.foodKeyword(), resolvePlace(nearbyPhrase.place(), foods));
        }

        return new SearchIntent(keyword, null);
    }

    private NearbyPhrase parseNearbyPhrase(String keyword) {
        if (keyword == null) {
            return null;
        }
        String value = keyword.trim();
        if (value.isBlank()) {
            return null;
        }

        for (String marker : List.of("附近", "周边", "nearby", "near", "around")) {
            int index = value.toLowerCase(Locale.ROOT).indexOf(marker);
            if (index > 0) {
                String place = value.substring(0, index).trim();
                String foodKeyword = stripGenericFoodTerms(value.substring(index + marker.length()));
                return new NearbyPhrase(place, foodKeyword);
            }
        }
        return null;
    }

    private String stripGenericFoodTerms(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.toLowerCase(Locale.ROOT)
                .replace("餐馆", "")
                .replace("餐厅", "")
                .replace("饭店", "")
                .replace("美食", "")
                .replace("吃饭", "")
                .replace("restaurant", "")
                .replace("restaurants", "")
                .replace("food", "")
                .replace("foods", "")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private LatLng resolvePlace(String place, List<Food> foods) {
        String key = placeKey(place);
        LatLng known = KNOWN_PLACES.get(key);
        if (known != null) {
            return known;
        }
        LatLng placeAnchor = resolvePlaceAnchor(key);
        if (placeAnchor != null) {
            return placeAnchor;
        }
        for (Food food : foods) {
            if (food.getDestination() == null || food.getDestination().getLatitude() == null || food.getDestination().getLongitude() == null) {
                continue;
            }
            String destinationKey = placeKey(food.getDestination().getName());
            if (destinationKey.contains(key) || key.contains(destinationKey)) {
                return new LatLng(food.getDestination().getLatitude(), food.getDestination().getLongitude());
            }
        }
        return null;
    }

    private LatLng resolvePlaceAnchor(String key) {
        List<FoodPlaceAnchor> anchors = foodRepository.findPlaceAnchors();
        if (anchors == null) {
            return null;
        }
        return anchors.stream()
                .filter(anchor -> anchor.getName() != null && anchor.getLatitude() != null && anchor.getLongitude() != null)
                .filter(anchor -> {
                    String anchorKey = placeKey(anchor.getName());
                    return anchorKey.contains(key) || key.contains(anchorKey);
                })
                .findFirst()
                .map(anchor -> new LatLng(anchor.getLatitude(), anchor.getLongitude()))
                .orElse(null);
    }

    private String placeKey(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private Food withDistance(Food food, LatLng anchor) {
        food.setDistanceMeters(null);
        if (anchor == null) {
            return food;
        }
        LatLng location = foodLocation(food);
        if (location != null) {
            food.setDistanceMeters(haversineMeters(anchor.lat(), anchor.lng(), location.lat(), location.lng()));
        }
        return food;
    }

    private LatLng foodLocation(Food food) {
        if (food.getLatitude() != null && food.getLongitude() != null) {
            return new LatLng(food.getLatitude(), food.getLongitude());
        }
        if (food.getDestination() != null
                && food.getDestination().getLatitude() != null
                && food.getDestination().getLongitude() != null) {
            return new LatLng(food.getDestination().getLatitude(), food.getDestination().getLongitude());
        }
        return null;
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

    private double destinationHeat(Food food) {
        return food.getDestination() == null ? 0 : safe(food.getDestination().getHeat());
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }

    private record LatLng(double lat, double lng) {
    }

    private record NearbyPhrase(String place, String foodKeyword) {
    }

    private record SearchIntent(String foodKeyword, LatLng anchor) {
    }

    private record PriceRange(Double min, Double max) {
    }
}
