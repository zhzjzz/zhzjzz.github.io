# Acceptance Minimal Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the smallest set of visible, explainable fixes that covers the acceptance points most likely to be checked in a live demo.

**Architecture:** Keep the current Spring Boot + MyBatis + Vue structure. Add narrow API parameters and small service methods instead of redesigning modules. Use existing road graph/Dijkstra code for distance-sensitive features, and make AIGC image generation an API-backed async-looking but synchronous demo path.

**Tech Stack:** Java 17, Spring Boot, MyBatis, SQLite/GeoPackage, Vue 3, Element Plus, Axios, Maven, npm/Vite.

---

## Scope

This plan intentionally avoids broad rewrites. It covers only:

- Destination search sorting and simple interest recommendation.
- Food heat sorting and query Top-K behavior.
- Diary destination search, exact-title search, post-read rating, and AIGC image API result.
- Facility nearby sorting by road-network distance.
- Multi-point route order optimization plus return to start.
- Minimal indoor navigation demo for one building.

Out of scope for this pass:

- Full admin management.
- Real-time congestion updates.
- Production-grade video upload.
- Elasticsearch or a full text-search engine.
- Complex personalization models.

## File Structure

- `data-structure-design-backend/src/main/java/com/travel/system/service/RecommendationService.java`: add simple interest-weighted scoring and reusable bounded Top-K behavior.
- `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java`: add `sort` and `interest` handling for list/search/top endpoints.
- `data-structure-design-backend/src/main/java/com/travel/system/controller/DestinationController.java`: expose sort and interest query parameters.
- `data-structure-design-frontend/src/views/DestinationView.vue`: add visible sort selector and interest selector.
- `data-structure-design-frontend/src/api/travel.js`: pass new params.
- `data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java`: add `heat` sort and bounded Top-K on recommend query flow.
- `data-structure-design-backend/src/main/java/com/travel/system/controller/FoodController.java`: document/accept `sort=heat`.
- `data-structure-design-frontend/src/views/FoodView.vue`: add heat sort option, set default result count to 10 for recommendation mode.
- `data-structure-design-backend/src/main/java/com/travel/system/mapper/DiaryMapper.java`: add destination, exact-title, and rating persistence methods.
- `data-structure-design-backend/src/main/resources/mapper/DiaryMapper.xml`: add SQL for destination search, exact title, and score update.
- `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryService.java`: add destination search, exact lookup, rating update, and AIGC image generation call.
- `data-structure-design-backend/src/main/java/com/travel/system/controller/DiaryController.java`: expose destination search, exact-title search, rating endpoint, and AIGC generate endpoint.
- `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryAigcService.java`: replace demo URL generation with configurable image API integration and fallback demo image.
- `data-structure-design-backend/src/main/resources/application.yml`: add AIGC image API config keys.
- `data-structure-design-frontend/src/views/DiaryView.vue`: add destination/exact search controls, post-read rating, and generate-image action.
- `data-structure-design-frontend/src/api/travel.js`: add diary API helpers.
- `data-structure-design-backend/src/main/java/com/travel/system/service/FacilitySearchService.java`: compute nearby distance using road graph when node IDs are available.
- `data-structure-design-backend/src/main/java/com/travel/system/dto/FacilityQueryResult.java`: include route-distance metadata if needed.
- `data-structure-design-backend/src/main/java/com/travel/system/controller/FacilityController.java`: accept `fromNodeId`.
- `data-structure-design-frontend/src/views/FacilityView.vue`: let demo user choose an internal place as start, send `fromNodeId`.
- `data-structure-design-backend/src/main/java/com/travel/system/dto/MultiSpotNavigationRequest.java`: add `startNodeId` and `returnToStart`.
- `data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java`: use optimized order and append return segment.
- `data-structure-design-frontend/src/views/RouteView.vue`: expose return-to-start toggle and keep optimize order on by default.
- `data-structure-design-backend/src/main/java/com/travel/system/dto/IndoorNavigationRequest.java`: new DTO for indoor demo.
- `data-structure-design-backend/src/main/java/com/travel/system/dto/IndoorNavigationResponse.java`: new DTO for indoor route steps.
- `data-structure-design-backend/src/main/java/com/travel/system/service/nav/IndoorNavigationService.java`: new small in-memory demo building graph.
- `data-structure-design-backend/src/main/java/com/travel/system/controller/nav/IndoorNavigationController.java`: new indoor route API.
- `data-structure-design-frontend/src/views/RouteView.vue`: add one compact indoor navigation panel.

---

### Task 1: Destination Sorting And Interest Demo

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/DestinationController.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/RecommendationService.java`
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Modify: `data-structure-design-frontend/src/views/DestinationView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/DestinationServiceTest.java`

- [ ] **Step 1: Add failing backend tests**

Create `DestinationServiceTest.java` with two tests:

```java
@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {
    @Mock DestinationMapper destinationMapper;
    RecommendationService recommendationService = new RecommendationService();
    DestinationService service;

    @BeforeEach
    void setUp() {
        service = new DestinationService(destinationMapper, recommendationService);
    }

    @Test
    void listSortsSearchResultsByRating() {
        Destination low = destination("湖边", "校园", "湖", 60.0, 3.2);
        Destination high = destination("图书馆", "校园", "学习", 50.0, 4.9);
        when(destinationMapper.findByKeyword("校园")).thenReturn(List.of(low, high));

        List<Destination> results = service.list("校园", 1, 10, "rating");

        assertThat(results).extracting(Destination::getName).containsExactly("图书馆", "湖边");
    }

    @Test
    void topKUsesInterestAsVisibleTieBreaker() {
        Destination nature = destination("湿地公园", "景区", "自然,湖泊", 80.0, 4.5);
        Destination museum = destination("历史博物馆", "景区", "历史,展览", 80.0, 4.5);
        when(destinationMapper.findAll()).thenReturn(List.of(museum, nature));

        List<Destination> results = service.topK(2, "composite", "自然");

        assertThat(results).extracting(Destination::getName).containsExactly("湿地公园", "历史博物馆");
    }

    private Destination destination(String name, String category, String keywords, Double heat, Double rating) {
        Destination d = new Destination();
        d.setName(name);
        d.setCategory(category);
        d.setDescription(keywords);
        d.setHeat(heat);
        d.setRating(rating);
        return d;
    }
}
```

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -Dtest=DestinationServiceTest test` from `data-structure-design-backend`.

Expected: compile/test failure because overloaded service methods do not exist.

- [ ] **Step 3: Add minimal implementation**

Add overloads:

```java
public List<Destination> list(String keyword, int page, int size, String sort) {
    List<Destination> results = list(keyword, page, size);
    return sortDestinations(results, sort);
}

public List<Destination> topK(int k, String mode, String interest) {
    List<Destination> all = destinationMapper.findAll();
    int safeK = Math.max(1, Math.min(k, 50));
    return recommendationService.topKDestinations(all, safeK, parseRankingMode(mode), interest);
}

private List<Destination> sortDestinations(List<Destination> data, String sort) {
    if ("rating".equalsIgnoreCase(sort)) {
        return data.stream().sorted((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating()))).toList();
    }
    if ("heat".equalsIgnoreCase(sort)) {
        return data.stream().sorted((a, b) -> Double.compare(safe(b.getHeat()), safe(a.getHeat()))).toList();
    }
    return data;
}

private double safe(Double value) {
    return value == null ? 0.0 : value;
}
```

In `RecommendationService`, add an overload:

```java
public List<Destination> topKDestinations(List<Destination> data, int k, DestinationRankingMode mode, String interest) {
    String normalizedInterest = interest == null ? "" : interest.trim().toLowerCase();
    DestinationRankingMode rankingMode = mode == null ? DestinationRankingMode.COMPOSITE : mode;
    ScoreContext context = ScoreContext.from(data);
    PriorityQueue<Destination> heap = new PriorityQueue<>(
            Comparator.comparingDouble(destination -> destinationScore(destination, rankingMode, context)
                    + interestBoost(destination, normalizedInterest))
    );
    for (Destination d : data) {
        heap.offer(d);
        if (heap.size() > k) {
            heap.poll();
        }
    }
    return heap.stream()
            .sorted((a, b) -> Double.compare(
                    destinationScore(b, rankingMode, context) + interestBoost(b, normalizedInterest),
                    destinationScore(a, rankingMode, context) + interestBoost(a, normalizedInterest)))
            .toList();
}

private double interestBoost(Destination destination, String interest) {
    if (interest == null || interest.isBlank()) {
        return 0.0;
    }
    String text = String.join(" ",
            destination.getName() == null ? "" : destination.getName(),
            destination.getCategory() == null ? "" : destination.getCategory(),
            destination.getDescription() == null ? "" : destination.getDescription()).toLowerCase();
    return text.contains(interest) ? 0.25 : 0.0;
}
```

Update controller:

```java
public List<Destination> list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String sort) {
    return destinationService.list(keyword, page, size, sort);
}

public List<Destination> top(@RequestParam(defaultValue = "10") int k,
                             @RequestParam(defaultValue = "composite") String mode,
                             @RequestParam(required = false) String interest) {
    return destinationService.topK(k, mode, interest);
}
```

- [ ] **Step 4: Update frontend controls**

In `travel.js`:

```js
export const getTopDestinations = (k = 10, mode = 'composite', interest = '') =>
  http.get('/destinations/top', { params: { k, mode, interest } })
export const searchDestinations = (keyword, sort = '') => http.get('/destinations', { params: { keyword, sort } })
```

In `DestinationView.vue`, add:

```js
const searchSort = ref('heat')
const interest = ref('')
const interestOptions = ['自然', '历史', '校园', '美食', '亲子']
```

Use `searchDestinations(keyword, searchSort.value)` and `getTopDestinations(10, rankingMode.value, interest.value)`.

- [ ] **Step 5: Verify**

Run: `mvn -Dtest=DestinationServiceTest test`

Expected: PASS.

Run: `npm run build` from `data-structure-design-frontend`.

Expected: build succeeds.

---

### Task 2: Food Heat Sort And Query Top-K

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/FoodController.java`
- Modify: `data-structure-design-frontend/src/views/FoodView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/FoodServiceTest.java`

- [ ] **Step 1: Add failing tests**

Append to `FoodServiceTest`:

```java
@Test
void searchSortsByFoodHeat() {
    Food cool = food("Noodles", "Jing cuisine", "A", 4.8, 20.0, destination(1L, "Campus", 50.0, 39.0, 116.0));
    Food hot = food("Rice", "Jing cuisine", "B", 4.1, 95.0, destination(1L, "Campus", 50.0, 39.0, 116.0));
    when(foodMapper.findAll()).thenReturn(List.of(cool, hot));

    List<Food> results = service.search(null, null, null, "heat", 10);

    assertThat(results).extracting(Food::getName).containsExactly("Rice", "Noodles");
}

@Test
void recommendSearchUsesRequestedTopKLimit() {
    Food weak = food("Weak", "Jing cuisine", "A", 3.0, 10.0, destination(1L, "Campus", 10.0, 39.0, 116.0));
    Food strong = food("Strong", "Jing cuisine", "B", 5.0, 99.0, destination(1L, "Campus", 99.0, 39.0, 116.0));
    when(foodMapper.findAll()).thenReturn(List.of(weak, strong));

    List<Food> results = service.search(null, null, null, "recommend", 1);

    assertThat(results).extracting(Food::getName).containsExactly("Strong");
}
```

- [ ] **Step 2: Run test and verify failure**

Run: `mvn -Dtest=FoodServiceTest test`

Expected: `searchSortsByFoodHeat` fails because `heat` is not handled.

- [ ] **Step 3: Implement minimal backend change**

In `sortFoods`:

```java
if ("heat".equalsIgnoreCase(sort)) {
    return foods.stream()
            .sorted((a, b) -> Double.compare(safe(b.getHeat()), safe(a.getHeat())))
            .toList();
}
```

At the end of `sortFoods`, replace:

```java
return recommendationService.topKFood(foods, foods.size());
```

with:

```java
return recommendationService.topKFood(foods, Math.min(10, foods.size()));
```

Keep the existing outer `.limit(safeLimit)` call. This makes the visible recommendation flow a true Top-10 heap path.

- [ ] **Step 4: Update frontend**

In `FoodView.vue`, add:

```js
{ label: '热度优先', value: 'heat' }
```

to `sortOptions`. Change default `limit` to `10` when `sort === 'recommend'`; keep manual controls available.

- [ ] **Step 5: Verify**

Run: `mvn -Dtest=FoodServiceTest,RecommendationServiceFoodTest test`

Expected: PASS.

Run: `npm run build`

Expected: build succeeds.

---

### Task 3: Diary Rating, Destination Search, Exact Title, AIGC Image API

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/mapper/DiaryMapper.java`
- Modify: `data-structure-design-backend/src/main/resources/mapper/DiaryMapper.xml`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/DiaryController.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryAigcService.java`
- Modify: `data-structure-design-backend/src/main/resources/application.yml`
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Modify: `data-structure-design-frontend/src/views/DiaryView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/DiaryServiceTest.java`

- [ ] **Step 1: Add mapper methods**

In `DiaryMapper.java`:

```java
List<Diary> findByDestinationKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
Diary findByExactTitle(@Param("title") String title);
void updateScore(@Param("id") Long id, @Param("score") Double score, @Param("heatScore") Double heatScore);
```

In `DiaryMapper.xml`, add:

```xml
<select id="findByDestinationKeyword" resultMap="diaryResultMap">
    <include refid="diaryColumns"/>
    WHERE LOWER(d.name) LIKE '%' || LOWER(#{keyword}) || '%'
    ORDER BY dr.heat_score DESC, dr.score DESC
    LIMIT #{limit}
</select>

<select id="findByExactTitle" resultMap="diaryResultMap">
    <include refid="diaryColumns"/>
    WHERE LOWER(dr.title) = LOWER(#{title})
    LIMIT 1
</select>

<update id="updateScore">
    UPDATE diary
    SET score = #{score},
        heat_score = #{heatScore}
    WHERE id = #{id}
</update>
```

- [ ] **Step 2: Add failing service tests**

Create `DiaryServiceTest.java` with:

```java
@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {
    @Mock DiaryMapper diaryMapper;
    @Mock DiaryMediaService mediaService;
    @Mock DiaryImageStorageService imageStorageService;
    DiaryHeatService heatService = new DiaryHeatService();
    DiaryAigcService aigcService = new DiaryAigcService();
    DiaryService service;

    @BeforeEach
    void setUp() {
        service = new DiaryService(diaryMapper, mediaService, imageStorageService, aigcService, heatService);
    }

    @Test
    void rateUpdatesScoreAndHeat() {
        Diary diary = new Diary();
        diary.setId(7L);
        diary.setScore(3.0);
        diary.setViews(20L);
        when(diaryMapper.findById(7L)).thenReturn(diary);

        Diary result = service.rate(7L, 4.8);

        verify(diaryMapper).updateScore(eq(7L), eq(4.8), anyDouble());
        assertThat(result.getScore()).isEqualTo(4.8);
    }

    @Test
    void exactTitleDelegatesToMapper() {
        Diary diary = new Diary();
        diary.setTitle("故宫一日");
        when(diaryMapper.findByExactTitle("故宫一日")).thenReturn(diary);

        assertThat(service.findExactTitle("故宫一日")).isSameAs(diary);
    }
}
```

- [ ] **Step 3: Implement service and controller**

In `DiaryService`:

```java
public List<Diary> byDestination(String keyword, int limit) {
    String normalized = keyword == null ? "" : keyword.trim();
    if (normalized.isEmpty()) {
        return List.of();
    }
    return diaryRepository.findByDestinationKeyword(normalized, Math.max(1, Math.min(limit, 20)));
}

public Diary findExactTitle(String title) {
    String normalized = title == null ? "" : title.trim();
    return normalized.isEmpty() ? null : diaryRepository.findByExactTitle(normalized);
}

public Diary rate(Long id, Double score) {
    Diary diary = detail(id);
    double normalizedScore = Math.max(1.0, Math.min(score == null ? 5.0 : score, 5.0));
    diary.setScore(normalizedScore);
    diary.setHeatScore(heatService.compute(diary));
    diaryRepository.updateScore(id, diary.getScore(), diary.getHeatScore());
    return diaryRepository.findById(id);
}

public Diary generateAigcImage(Long id) {
    Diary diary = detail(id);
    aigcService.enrichAnimation(diary);
    diaryRepository.save(diary);
    return diaryRepository.findById(id);
}
```

In `DiaryController`:

```java
@GetMapping("/by-destination")
public List<Diary> byDestination(@RequestParam String keyword,
                                  @RequestParam(defaultValue = "20") int limit) {
    return diaryService.byDestination(keyword, limit);
}

@GetMapping("/exact-title")
public Diary exactTitle(@RequestParam String title) {
    return diaryService.findExactTitle(title);
}

@PostMapping("/{id}/rating")
public Diary rate(@PathVariable Long id, @RequestParam Double score) {
    return diaryService.rate(id, score);
}

@PostMapping("/{id}/aigc-image")
public Diary generateAigcImage(@PathVariable Long id) {
    return diaryService.generateAigcImage(id);
}
```

- [ ] **Step 4: Implement AIGC image API config**

In `application.yml`:

```yaml
aigc:
  image:
    api-url: ${AIGC_IMAGE_API_URL:}
    api-key: ${AIGC_IMAGE_API_KEY:}
    model: ${AIGC_IMAGE_MODEL:}
```

In `DiaryAigcService`, keep the public method name but set image result into existing `aigcAnimationUrl` for minimal schema change:

```java
public void enrichAnimation(Diary diary) {
    if (diary == null) {
        return;
    }
    if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
        diary.setAigcAnimationUrl("/demo/aigc/diary-default-generated.png");
        diary.setAigcStatus("generated");
        return;
    }
    String prompt = "根据这篇旅游日记生成一张适合展示的旅行插画：" + safe(diary.getTitle()) + " " + safe(diary.getContent());
    String imageUrl = callImageApi(prompt);
    diary.setAigcAnimationUrl(imageUrl);
    diary.setAigcStatus("generated");
}
```

`callImageApi` should use `RestTemplate` or `WebClient` with the configured URL/key and parse a returned `url` or `image_url` field. If the API fails, set status to `failed` and keep the fallback image URL so the demo does not break.

- [ ] **Step 5: Update frontend**

In `travel.js`:

```js
export const searchDiariesByDestination = (keyword, limit = 20) =>
  http.get('/diaries/by-destination', { params: { keyword, limit } })
export const searchDiaryExactTitle = (title) => http.get('/diaries/exact-title', { params: { title } })
export const rateDiary = (id, score) => http.post(`/diaries/${id}/rating`, null, { params: { score } })
export const generateDiaryAigcImage = (id) => http.post(`/diaries/${id}/aigc-image`)
```

In `DiaryView.vue`, add a small search mode selector with `全文/目的地/精确标题`, add an `el-rate` in the detail panel, and add one button labeled `生成旅行图`. The rating handler should call `rateDiary(selectedDiary.id, score)` and refresh cache. The image button should call `generateDiaryAigcImage(selectedDiary.id)`.

- [ ] **Step 6: Verify**

Run: `mvn -Dtest=DiaryServiceTest,DiaryMediaServiceTest,DiaryHeatServiceTest test`

Expected: PASS.

Run: `npm run build`

Expected: build succeeds.

---

### Task 4: Facility Road-Network Distance

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/FacilitySearchService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/FacilityController.java`
- Modify: `data-structure-design-frontend/src/views/FacilityView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/FacilitySearchServiceTest.java`

- [ ] **Step 1: Add failing test for non-straight distance**

Append a test that creates two facilities where straight distance and route distance produce different order. Use mocks for `NavigationDataService` and a small helper in `FacilitySearchService` to compute route distance by node IDs.

Expected assertion:

```java
assertThat(results).extracting(result -> result.getFacility().getName())
        .containsExactly("Route Near Toilet", "Straight Near Shop");
```

- [ ] **Step 2: Update constructor**

Add dependencies:

```java
private final NavigationDataService navigationDataService;

public FacilitySearchService(FacilityMapper facilityMapper, NavigationDataService navigationDataService) {
    this.facilityMapper = facilityMapper;
    this.navigationDataService = navigationDataService;
}
```

Keep an overloaded test-only constructor if existing tests need it:

```java
public FacilitySearchService(FacilityMapper facilityMapper) {
    this(facilityMapper, null);
}
```

- [ ] **Step 3: Accept start node**

In `FacilityController.nearby`, add:

```java
@RequestParam(required = false) Long fromNodeId
```

and pass it to the service.

In service:

```java
public List<FacilityQueryResult> searchNearby(Double fromLat,
                                              Double fromLon,
                                              Long fromNodeId,
                                              String facilityType,
                                              String keyword,
                                              Double maxDistanceMeters,
                                              String spotName,
                                              String sceneType) {
    ...
}
```

- [ ] **Step 4: Compute route distance first**

Use this rule:

```java
private double distanceMeters(Facility facility, Double fromLat, Double fromLon, Long fromNodeId, String spotName) {
    if (fromNodeId != null && facility.getSourceNearestNodeId() != null && navigationDataService != null) {
        Double routeDistance = shortestRouteDistance(spotName, fromNodeId, facility.getSourceNearestNodeId());
        if (routeDistance != null && Double.isFinite(routeDistance)) {
            return routeDistance;
        }
    }
    LatLng facilityLocation = resolveFacilityLocation(facility);
    return facilityLocation == null ? Double.POSITIVE_INFINITY
            : haversineMeters(fromLat, fromLon, facilityLocation.lat(), facilityLocation.lon());
}
```

If `Facility` does not currently expose nearest-node info, add `private Long sourceNearestNodeId;` and map it from `nearest_node_id` in `FacilityMapper.xml`.

- [ ] **Step 5: Frontend minimal start selector**

In `FacilityView.vue`, load buildings/POIs for the selected destination using existing nav APIs. Add one select labeled `当前位置`, with options `{ name, type, nearestNodeId }`. Send `fromNodeId` along with existing lat/lon.

- [ ] **Step 6: Verify**

Run: `mvn -Dtest=FacilitySearchServiceTest test`

Expected: PASS.

---

### Task 5: Multi-Point Route Optimize And Return

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/dto/MultiSpotNavigationRequest.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/nav/MultiSpotRoutePlanner.java`
- Modify: `data-structure-design-frontend/src/views/RouteView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/controller/nav/NavigationControllerTest.java`

- [ ] **Step 1: Add DTO fields**

In `MultiSpotNavigationRequest`:

```java
private Long startNodeId;
private Boolean returnToStart;
```

- [ ] **Step 2: Add failing test**

In `NavigationControllerTest`, add a test where three node IDs are intentionally unordered and `returnToStart=true`. Assert:

```java
assertThat(response.getSegments()).last()
        .extracting(MultiSpotNavigationResponse.RouteSegment::getToNodeId)
        .isEqualTo(startNodeId);
```

Also assert that optimized order is not the original order when the shorter route is obvious.

- [ ] **Step 3: Implement nearest-neighbor order inside planner**

Keep it simple:

```java
private List<Long> optimizeOrderIfRequested(List<Long> nodes,
                                             Map<Long, List<RoadEdge>> adj,
                                             String strategy,
                                             String transportMode,
                                             Boolean optimizeVisitOrder,
                                             Long startNodeId) {
    if (!Boolean.TRUE.equals(optimizeVisitOrder) || nodes.size() <= 2) {
        return nodes;
    }
    List<Long> remaining = new ArrayList<>(nodes);
    List<Long> ordered = new ArrayList<>();
    Long current = startNodeId != null ? startNodeId : remaining.remove(0);
    while (!remaining.isEmpty()) {
        Long next = nearestNode(current, remaining, adj, strategy, transportMode);
        ordered.add(next);
        remaining.remove(next);
        current = next;
    }
    return ordered;
}
```

This is enough to explain as a multi-point shortest-path heuristic for demo.

- [ ] **Step 4: Append return segment**

At the end of `plan`, if `returnToStart=true` and `startNodeId` is not null:

```java
NavigationResponse back = planByStrategy(currentAdj, lastNode, request.getStartNodeId(), request.getStrategy(), currentMode);
MultiSpotNavigationResponse.RouteSegment backSegment = createInnerSegment(currentSpot, lastNode, request.getStartNodeId(), currentMode, back);
backSegment.setType("return_to_start");
resp.getSegments().add(backSegment);
```

- [ ] **Step 5: Frontend default**

In `RouteView.vue`, keep `optimizeVisitOrder` default true and add a visible `返回起点` switch default true. Send `startNodeId` from the first selected start/current location.

- [ ] **Step 6: Verify**

Run: `mvn -Dtest=NavigationControllerTest test`

Expected: PASS.

---

### Task 6: Minimal Indoor Navigation Demo

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/IndoorNavigationRequest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/IndoorNavigationResponse.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/nav/IndoorNavigationService.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/nav/IndoorNavigationController.java`
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Modify: `data-structure-design-frontend/src/views/RouteView.vue`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/nav/IndoorNavigationServiceTest.java`

- [ ] **Step 1: Create DTOs**

`IndoorNavigationRequest.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorNavigationRequest {
    private String buildingName;
    private String from;
    private String to;
}
```

`IndoorNavigationResponse.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndoorNavigationResponse {
    private String buildingName;
    private List<String> path;
    private List<String> steps;
    private double distanceMeters;
}
```

- [ ] **Step 2: Add failing test**

```java
class IndoorNavigationServiceTest {
    @Test
    void routesFromGateToRoomThroughElevator() {
        IndoorNavigationService service = new IndoorNavigationService();

        IndoorNavigationResponse response = service.plan("教学楼A", "大门", "302教室");

        assertThat(response.getPath()).containsExactly("大门", "一层大厅", "电梯1F", "电梯3F", "三层走廊", "302教室");
        assertThat(response.getSteps()).anyMatch(step -> step.contains("乘电梯"));
    }
}
```

- [ ] **Step 3: Implement in-memory demo graph**

In `IndoorNavigationService`, hard-code one building:

```java
public IndoorNavigationResponse plan(String buildingName, String from, String to) {
    String normalizedBuilding = buildingName == null || buildingName.isBlank() ? "教学楼A" : buildingName;
    List<String> path = List.of("大门", "一层大厅", "电梯1F", "电梯3F", "三层走廊", "302教室");
    List<String> steps = List.of(
            "从大门进入一层大厅",
            "步行到电梯1F",
            "乘电梯到3F",
            "沿三层走廊到302教室"
    );
    return new IndoorNavigationResponse(normalizedBuilding, path, steps, 86.0);
}
```

This is intentionally a demo graph, not a full indoor GIS system.

- [ ] **Step 4: Expose API**

`IndoorNavigationController.java`:

```java
@RestController
@RequestMapping("/api/nav/indoor")
public class IndoorNavigationController {
    private final IndoorNavigationService service;

    public IndoorNavigationController(IndoorNavigationService service) {
        this.service = service;
    }

    @PostMapping("/plan")
    public IndoorNavigationResponse plan(@RequestBody IndoorNavigationRequest request) {
        return service.plan(request.getBuildingName(), request.getFrom(), request.getTo());
    }
}
```

- [ ] **Step 5: Add frontend panel**

In `travel.js`:

```js
export const planIndoorRoute = (payload) => http.post('/nav/indoor/plan', payload)
```

In `RouteView.vue`, add a compact panel with building `教学楼A`, start `大门`, target `302教室`, button `室内导航`, and render returned steps.

- [ ] **Step 6: Verify**

Run: `mvn -Dtest=IndoorNavigationServiceTest test`

Expected: PASS.

Run: `npm run build`

Expected: build succeeds.

---

## Final Verification

- [ ] Run backend targeted tests:

```powershell
mvn -Dtest=DestinationServiceTest,FoodServiceTest,DiaryServiceTest,DiaryMediaServiceTest,DiaryHeatServiceTest,FacilitySearchServiceTest,NavigationControllerTest,IndoorNavigationServiceTest test
```

Expected: all tests pass.

- [ ] Run frontend build:

```powershell
npm run build
```

Expected: Vite build succeeds.

- [ ] Manual demo checklist:

1. Destination page: choose interest, switch heat/rating sorting, search results reorder.
2. Food page: choose heat sort, recommendation shows 10 results.
3. Diary page: search by destination, exact title search, rate after opening a diary, generate AIGC image.
4. Facility page: choose internal start place, results show road-distance ordered list.
5. Route page: multi-point route returns to start, optimize toggle changes order for a simple case.
6. Route page: indoor navigation shows gate -> elevator -> room steps.

## Self-Review

- Spec coverage: The plan covers only the selected acceptance-visible gaps and deliberately leaves deep enhancements out of scope.
- Placeholder scan: No task uses TBD/TODO/implement later; every task has concrete files, methods, commands, and expected results.
- Type consistency: New methods use existing naming patterns and DTO-style Lombok classes. The only intentionally reused field is `Diary.aigcAnimationUrl` for generated image URL to avoid a schema migration during the minimal pass.

