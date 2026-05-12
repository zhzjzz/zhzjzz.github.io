# Food Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a visitor-facing food page with search, filters, recommendations, and enough stable demo data for course presentation.

**Architecture:** Extend the existing Spring Boot/MyBatis food module instead of adding a parallel stack. The backend will expose query parameters on `/api/foods`, keep `/api/foods/top` for the homepage, and provide idempotent seed data. The Vue frontend will add a dedicated `/foods` page and wire it into the existing navigation.

**Tech Stack:** Java 17, Spring Boot, MyBatis XML mappers, JUnit 5, Mockito, Vue 3, Vite, Element Plus, IconPark icons.

---

## File Structure

- Modify `data-structure-design-backend/src/main/java/com/travel/system/model/Food.java`
  - Add `heat` so food recommendations can use a direct popularity signal.
- Modify `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
  - Add `heat REAL` to the `food` table definition.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/mapper/FoodMapper.java`
  - Add query methods for cuisines and destination-aware filtering.
- Modify `data-structure-design-backend/src/main/resources/mapper/FoodMapper.xml`
  - Map `heat`, support filters, and preserve destination joins.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java`
  - Add filter/sort/limit orchestration and safe defaults.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/service/RecommendationService.java`
  - Use bounded Top-K with safe `k`, rating, food heat, and destination heat.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/controller/FoodController.java`
  - Expose visitor query params.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/service/DataInitializer.java`
  - Add idempotent destination and food seed data.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/FoodServiceTest.java`
  - Test filtering, sorting, and limit behavior with mocked mapper data.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/RecommendationServiceFoodTest.java`
  - Test Top-K bounds and ordering.
- Modify `data-structure-design-frontend/src/api/travel.js`
  - Add `searchFoods` and `listFoodCuisines`.
- Create `data-structure-design-frontend/src/views/FoodView.vue`
  - Visitor-facing food search and recommendation page.
- Modify `data-structure-design-frontend/src/router/index.js`
  - Add `/foods` route.
- Modify `data-structure-design-frontend/src/components/AppNav.vue`
  - Add "美食" nav item and point "吃什么" to `/foods`.
- Modify `data-structure-design-frontend/src/views/HomeView.vue`
  - Keep preview section and link cards/entry points to `/foods`.

---

### Task 1: Backend Food Service Tests

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/FoodServiceTest.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/RecommendationServiceFoodTest.java`

- [ ] **Step 1: Write failing service tests**

Create `FoodServiceTest.java` with tests that define the desired behavior:

```java
package com.travel.system.service;

import com.travel.system.mapper.FoodMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodServiceTest {

    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final RecommendationService recommendationService = new RecommendationService();
    private final FoodService service = new FoodService(foodMapper, recommendationService);

    @Test
    void searchFiltersKeywordCuisineDestinationAndSortsByRating() {
        Destination campus = destination(1L, "北京邮电大学", 88d);
        Destination park = destination(2L, "紫竹院公园", 72d);
        Food noodles = food(1L, "老北京炸酱面", "京菜", "校园食堂一层", 4.6, 91d, campus);
        Food coffee = food(2L, "湖畔拿铁", "咖啡", "湖畔咖啡", 4.8, 66d, park);
        Food rice = food(3L, "招牌鸡腿饭", "快餐", "学苑餐厅", 4.4, 80d, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, coffee, rice));

        List<Food> results = service.search("饭", "快餐", 1L, "rating", 10);

        assertThat(results).extracting(Food::getName).containsExactly("招牌鸡腿饭");
    }

    @Test
    void searchUsesRecommendationSortAndLimitByDefault() {
        Destination campus = destination(1L, "北京邮电大学", 88d);
        Food noodles = food(1L, "老北京炸酱面", "京菜", "校园食堂一层", 4.6, 91d, campus);
        Food burger = food(2L, "牛肉芝士堡", "西式简餐", "学生活动中心轻食", 4.2, 95d, campus);
        Food dessert = food(3L, "桂花酒酿圆子", "甜品", "校园甜品铺", 4.9, 70d, campus);
        when(foodMapper.findAll()).thenReturn(List.of(noodles, burger, dessert));

        List<Food> results = service.search(null, null, null, "recommend", 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("老北京炸酱面");
    }

    private Destination destination(Long id, String name, Double heat) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setHeat(heat);
        return destination;
    }

    private Food food(Long id, String name, String cuisine, String storeName, Double rating, Double heat, Destination destination) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setCuisine(cuisine);
        food.setStoreName(storeName);
        food.setRating(rating);
        food.setHeat(heat);
        food.setDestination(destination);
        return food;
    }
}
```

Create `RecommendationServiceFoodTest.java`:

```java
package com.travel.system.service;

import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceFoodTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void topKFoodBoundsInvalidKAndOrdersByRecommendationScore() {
        Destination destination = new Destination();
        destination.setHeat(80d);

        Food strong = food("老北京炸酱面", 4.7, 96d, destination);
        Food weak = food("普通套餐", 3.9, 30d, destination);
        Food medium = food("湖畔拿铁", 4.5, 70d, destination);

        List<Food> results = service.topKFood(List.of(weak, medium, strong), -1);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(Food::getName)
                .containsExactly("老北京炸酱面", "湖畔拿铁", "普通套餐");
    }

    private Food food(String name, Double rating, Double heat, Destination destination) {
        Food food = new Food();
        food.setName(name);
        food.setRating(rating);
        food.setHeat(heat);
        food.setDestination(destination);
        return food;
    }
}
```

- [ ] **Step 2: Run tests and confirm they fail**

Run:

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=.m2 -f data-structure-design-backend\pom.xml -Dtest=FoodServiceTest,RecommendationServiceFoodTest test
```

Expected: compilation fails because `Food.getHeat/setHeat` and the new `FoodService.search(...)` signature do not exist yet.

---

### Task 2: Backend Food Query Implementation

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/model/Food.java`
- Modify: `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/mapper/FoodMapper.java`
- Modify: `data-structure-design-backend/src/main/resources/mapper/FoodMapper.xml`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/RecommendationService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/FoodController.java`

- [ ] **Step 1: Add `heat` to Food**

Add this field in `Food.java` after `rating`:

```java
private Double heat;
```

- [ ] **Step 2: Add `heat` to schema**

Change the `food` table in `schema-sqlite.sql` to:

```sql
CREATE TABLE IF NOT EXISTS food (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    cuisine TEXT,
    store_name TEXT,
    rating REAL,
    heat REAL,
    destination_id INTEGER
);
```

- [ ] **Step 3: Update mapper interface**

Keep the existing methods and add:

```java
List<String> findCuisines();
```

- [ ] **Step 4: Update mapper XML**

Add `heat` to `foodResultMap`, every select list, insert, and update:

```xml
<result column="heat" property="heat"/>
```

Insert should use:

```xml
INSERT INTO food (name, cuisine, store_name, rating, heat, destination_id)
VALUES (#{name}, #{cuisine}, #{storeName}, #{rating}, #{heat}, #{destination.id})
```

Update should set:

```xml
heat = #{heat},
```

Add cuisine query:

```xml
<select id="findCuisines" resultType="string">
    SELECT DISTINCT cuisine
    FROM food
    WHERE cuisine IS NOT NULL AND TRIM(cuisine) != ''
    ORDER BY cuisine
</select>
```

- [ ] **Step 5: Implement `FoodService.search` overload**

Add a new method:

```java
public List<Food> search(String keyword, String cuisine, Long destinationId, String sort, int limit) {
    String normalizedKeyword = normalize(keyword);
    String normalizedCuisine = normalize(cuisine);
    int safeLimit = limit <= 0 ? 30 : Math.min(limit, 100);
    List<Food> filtered = foodRepository.findAll().stream()
            .filter(food -> matchesKeyword(food, normalizedKeyword))
            .filter(food -> normalizedCuisine == null || normalizedCuisine.equalsIgnoreCase(normalize(food.getCuisine())))
            .filter(food -> destinationId == null || (food.getDestination() != null && destinationId.equals(food.getDestination().getId())))
            .toList();

    return sortFoods(filtered, sort).stream().limit(safeLimit).toList();
}
```

Add private helpers:

```java
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

private String normalize(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}

private List<Food> sortFoods(List<Food> foods, String sort) {
    if ("rating".equalsIgnoreCase(sort)) {
        return foods.stream()
                .sorted((a, b) -> Double.compare(safe(b.getRating()), safe(a.getRating())))
                .toList();
    }
    if ("destinationHeat".equalsIgnoreCase(sort)) {
        return foods.stream()
                .sorted((a, b) -> Double.compare(destinationHeat(b), destinationHeat(a)))
                .toList();
    }
    return recommendationService.topKFood(foods, foods.size());
}

private double destinationHeat(Food food) {
    return food.getDestination() == null ? 0 : safe(food.getDestination().getHeat());
}

private double safe(Double value) {
    return value == null ? 0 : value;
}
```

Keep the existing `search(String keyword, int page, int size)` for compatibility and have the controller use the new method.

- [ ] **Step 6: Make `RecommendationService.topKFood` safe**

Update `topKFood` so `k <= 0` returns all sorted results and food heat participates in scoring:

```java
public List<Food> topKFood(List<Food> data, int k) {
    int safeK = k <= 0 ? data.size() : Math.min(k, data.size());
    PriorityQueue<Food> heap = new PriorityQueue<>(Comparator.comparingDouble(this::foodScore));
    for (Food f : data) {
        heap.offer(f);
        if (heap.size() > safeK) {
            heap.poll();
        }
    }
    return heap.stream()
            .sorted((a, b) -> Double.compare(foodScore(b), foodScore(a)))
            .toList();
}
```

Update `foodScore`:

```java
private double foodScore(Food f) {
    Double destinationHeat = f.getDestination() == null ? null : f.getDestination().getHeat();
    return safe(f.getRating()) * 0.55 + safe(f.getHeat()) * 0.30 + safe(destinationHeat) * 0.15;
}
```

- [ ] **Step 7: Update controller**

Change `/api/foods` to accept:

```java
@RequestParam(required = false) String keyword,
@RequestParam(required = false) String cuisine,
@RequestParam(required = false) Long destinationId,
@RequestParam(defaultValue = "recommend") String sort,
@RequestParam(defaultValue = "30") int limit
```

Return:

```java
return foodService.search(keyword, cuisine, destinationId, sort, limit);
```

Add:

```java
@GetMapping("/cuisines")
public List<String> cuisines() {
    return foodService.cuisines();
}
```

- [ ] **Step 8: Run backend tests**

Run:

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=.m2 -f data-structure-design-backend\pom.xml -Dtest=FoodServiceTest,RecommendationServiceFoodTest test
```

Expected: both test classes pass.

- [ ] **Step 9: Commit backend query work**

Run:

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/model/Food.java data-structure-design-backend/src/main/resources/schema-sqlite.sql data-structure-design-backend/src/main/java/com/travel/system/mapper/FoodMapper.java data-structure-design-backend/src/main/resources/mapper/FoodMapper.xml data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java data-structure-design-backend/src/main/java/com/travel/system/service/RecommendationService.java data-structure-design-backend/src/main/java/com/travel/system/controller/FoodController.java data-structure-design-backend/src/test/java/com/travel/system/service/FoodServiceTest.java data-structure-design-backend/src/test/java/com/travel/system/service/RecommendationServiceFoodTest.java
git commit -m "feat: extend food search and recommendation"
```

---

### Task 3: Seed Demo Food Data

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DataInitializer.java`

- [ ] **Step 1: Update seed destination and food logic**

Add idempotent seed logic that creates several named destinations when destination data is absent, then inserts about 36 food rows when food count is below a presentation threshold.

Use destination examples:

```text
北京邮电大学沙河校区
紫竹院公园
颐和园
圆明园
奥林匹克公园
南锣鼓巷
```

Use food examples:

```text
老北京炸酱面, 京菜, 校园食堂一层, 4.6, 91
招牌鸡腿饭, 快餐, 学苑餐厅, 4.4, 80
湖畔拿铁, 咖啡, 湖畔咖啡, 4.8, 74
豌豆黄, 京味小吃, 南锣鼓巷小吃铺, 4.5, 85
桂花酒酿圆子, 甜品, 颐和园茶点铺, 4.7, 78
烤鸭卷, 京菜, 圆明园游客餐厅, 4.6, 82
```

Each row must set `name`, `cuisine`, `storeName`, `rating`, `heat`, and `destination`.

- [ ] **Step 2: Run backend application smoke test**

Run backend tests first:

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=.m2 -f data-structure-design-backend\pom.xml test
```

Expected: tests pass or only unrelated existing tests fail; if unrelated tests fail, record the failing test names before continuing.

- [ ] **Step 3: Commit seed data**

Run:

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/service/DataInitializer.java
git commit -m "feat: seed food demo data"
```

---

### Task 4: Frontend Food Page

**Files:**
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Create: `data-structure-design-frontend/src/views/FoodView.vue`
- Modify: `data-structure-design-frontend/src/router/index.js`
- Modify: `data-structure-design-frontend/src/components/AppNav.vue`
- Modify: `data-structure-design-frontend/src/views/HomeView.vue`

- [ ] **Step 1: Add frontend API helpers**

Add:

```js
export const searchFoods = (params = {}) => http.get('/foods', { params })
export const listFoodCuisines = () => http.get('/foods/cuisines')
```

- [ ] **Step 2: Create `FoodView.vue`**

Create a Vue page with:

```vue
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bowl, Refresh, Search } from '@icon-park/vue-next'
import { listDestinations, listFoodCuisines, searchFoods } from '../api/travel'
import foodDefaultImage from '../assets/defaults/food-default.png'

const loading = ref(false)
const foods = ref([])
const destinations = ref([])
const cuisines = ref([])
const form = ref({
  keyword: '',
  cuisine: '',
  destinationId: null,
  sort: 'recommend',
  limit: 30,
})

const resultTitle = computed(() => `${foods.value.length} 个美食结果`)

const loadOptions = async () => {
  const [destinationResponse, cuisineResponse] = await Promise.all([
    listDestinations(),
    listFoodCuisines(),
  ])
  destinations.value = Array.isArray(destinationResponse.data) ? destinationResponse.data : []
  cuisines.value = Array.isArray(cuisineResponse.data) ? cuisineResponse.data : []
}

const search = async () => {
  loading.value = true
  try {
    const params = {
      keyword: form.value.keyword.trim() || undefined,
      cuisine: form.value.cuisine || undefined,
      destinationId: form.value.destinationId || undefined,
      sort: form.value.sort,
      limit: form.value.limit,
    }
    const { data } = await searchFoods(params)
    foods.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error(error)
    foods.value = []
    ElMessage.error('美食数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const reset = async () => {
  form.value = {
    keyword: '',
    cuisine: '',
    destinationId: null,
    sort: 'recommend',
    limit: 30,
  }
  await search()
}

onMounted(async () => {
  await loadOptions()
  await search()
})
</script>
```

The template should include an Element Plus form, search/reset buttons, a result count, responsive cards, and `el-empty` for no result state. The card fields are `item.name`, `item.cuisine`, `item.storeName`, `item.rating`, `item.heat`, and `item.destination?.name`.

- [ ] **Step 3: Add route**

Import `FoodView` and add:

```js
{ path: "/foods", name: "foods", component: FoodView },
```

- [ ] **Step 4: Update nav**

Add a food nav item:

```js
{ label: '美食', path: '/foods' },
```

Add icon mapping:

```js
'/foods': Bowl,
```

Change the quick action target for "吃什么" to:

```js
{ label: '吃什么', aria: '打开美食推荐', target: '/foods' },
```

- [ ] **Step 5: Update homepage food entry**

Keep `getTopFoods(6)`, but make the section action and food cards route to `/foods`, not diary search.

- [ ] **Step 6: Run frontend build**

Run:

```powershell
npm.cmd run build
```

from:

```text
data-structure-design-frontend
```

Expected: Vite build succeeds.

- [ ] **Step 7: Commit frontend page**

Run:

```powershell
git add data-structure-design-frontend/src/api/travel.js data-structure-design-frontend/src/views/FoodView.vue data-structure-design-frontend/src/router/index.js data-structure-design-frontend/src/components/AppNav.vue data-structure-design-frontend/src/views/HomeView.vue
git commit -m "feat: add visitor food page"
```

---

### Task 5: End-To-End Verification

**Files:**
- No source changes expected unless verification reveals a bug.

- [ ] **Step 1: Run full backend tests**

Run:

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=.m2 -f data-structure-design-backend\pom.xml test
```

Expected: build success.

- [ ] **Step 2: Run frontend build**

Run from `data-structure-design-frontend`:

```powershell
npm.cmd run build
```

Expected: Vite build succeeds.

- [ ] **Step 3: Start local servers**

Start backend with the local SQLite file and frontend with Vite. Use the existing approved commands when possible.

- [ ] **Step 4: Browser check**

Open `/foods` in the in-app browser. Verify:

- Search returns matching cards.
- Cuisine filter narrows results.
- Destination filter narrows results when destinations exist.
- Sort dropdown changes order.
- Mobile-width layout does not overlap text.

- [ ] **Step 5: Commit any verification fixes**

If verification required source fixes, commit them:

```powershell
git add <changed source files>
git commit -m "fix: polish food module verification issues"
```

Do not add `dist`, `.m2`, `.tools`, or local log files unless explicitly requested.
