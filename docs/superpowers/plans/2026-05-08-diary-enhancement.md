# Tourism Diary Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the tourism diary module from basic create/search into a demo-ready content platform with creation storage, lossless media optimization metadata, AIGC animation simulation, heat scoring, and sharing/comment interactions.

**Architecture:** Keep the existing Spring Boot + MyBatis + Vue structure. Backend changes extend the existing `Diary` model and mapper, add small focused services for heat score, media optimization, and AIGC simulation, then expose simple REST endpoints. Frontend changes keep `DiaryView.vue` as the page owner and add API helpers in `travel.js`.

**Tech Stack:** Spring Boot, MyBatis XML mapper, SQLite/GeoPackage, Vue 3, Element Plus, Vite, PowerShell on Windows.

---

## File Structure

- Modify `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
  - Add diary enhancement columns and `diary_comment` table for share/community demo.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/model/Diary.java`
  - Add media optimization, AIGC, heat, and interaction fields.
- Create `data-structure-design-backend/src/main/java/com/travel/system/model/DiaryComment.java`
  - Represents comments shown under shared diaries.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryHeatService.java`
  - Pure heat-score calculation, easy to unit test.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryMediaService.java`
  - Demo-safe media optimization metadata generator. No binary file processing in this iteration.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryAigcService.java`
  - Demo-safe AIGC animation metadata generator. No external AI dependency.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryService.java`
  - Enrich diary save, sort hot diaries, update interaction counters, add comments.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/mapper/DiaryMapper.java`
  - Add hot listing, interaction update, and comment methods.
- Modify `data-structure-design-backend/src/main/resources/mapper/DiaryMapper.xml`
  - Map new fields and SQL statements.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/controller/DiaryController.java`
  - Add endpoints for hot diaries, interactions, comments, and share lookup.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/DiaryHeatServiceTest.java`
  - Unit tests for deterministic heat scoring.
- Modify `data-structure-design-frontend/src/api/travel.js`
  - Add helpers for hot diaries, interactions, comments, and share lookup.
- Modify `data-structure-design-frontend/src/views/DiaryView.vue`
  - Replace the current basic table page with a demo-ready creation/sharing interface and fix visible Chinese text.

---

### Task 1: Add Diary Data Fields

**Files:**
- Modify: `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/model/Diary.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/model/DiaryComment.java`

- [ ] **Step 1: Extend `schema-sqlite.sql`**

Add these columns to the `diary` table definition so fresh local databases have the enhanced shape:

```sql
CREATE TABLE IF NOT EXISTS diary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT,
    media_url TEXT,
    media_type TEXT,
    compressed_media_url TEXT,
    original_size_bytes INTEGER DEFAULT 0,
    compressed_size_bytes INTEGER DEFAULT 0,
    compression_status TEXT DEFAULT 'pending',
    aigc_animation_url TEXT,
    aigc_status TEXT DEFAULT 'pending',
    heat_score REAL DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    share_count INTEGER DEFAULT 0,
    is_public INTEGER DEFAULT 1,
    share_token TEXT,
    published_at TEXT,
    score REAL,
    views INTEGER,
    destination_id INTEGER
);

CREATE TABLE IF NOT EXISTS diary_comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    author_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TEXT NOT NULL
);
```

- [ ] **Step 2: Add compatible migration statements**

Append migration-safe statements after table creation because the user may already have a populated local database:

```sql
ALTER TABLE diary ADD COLUMN compressed_media_url TEXT;
ALTER TABLE diary ADD COLUMN original_size_bytes INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN compressed_size_bytes INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN compression_status TEXT DEFAULT 'pending';
ALTER TABLE diary ADD COLUMN aigc_animation_url TEXT;
ALTER TABLE diary ADD COLUMN aigc_status TEXT DEFAULT 'pending';
ALTER TABLE diary ADD COLUMN heat_score REAL DEFAULT 0;
ALTER TABLE diary ADD COLUMN like_count INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN favorite_count INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN comment_count INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN share_count INTEGER DEFAULT 0;
ALTER TABLE diary ADD COLUMN is_public INTEGER DEFAULT 1;
ALTER TABLE diary ADD COLUMN share_token TEXT;
```

SQLite will error if a column already exists. Put these statements behind the existing initializer pattern if the project executes schema manually; otherwise document that fresh database recreation is the supported demo path. Do not run destructive DB changes without user approval.

- [ ] **Step 3: Extend `Diary.java`**

Add fields below `mediaType` and before `score`:

```java
private String compressedMediaUrl;
private Long originalSizeBytes;
private Long compressedSizeBytes;
private String compressionStatus;
private String aigcAnimationUrl;
private String aigcStatus;
private Double heatScore;
private Long likeCount;
private Long favoriteCount;
private Long commentCount;
private Long shareCount;
private Boolean isPublic;
private String shareToken;
```

- [ ] **Step 4: Create `DiaryComment.java`**

```java
package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiaryComment {
    private Long id;
    private Long diaryId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Commit data model changes**

```powershell
git add data-structure-design-backend/src/main/resources/schema-sqlite.sql `
  data-structure-design-backend/src/main/java/com/travel/system/model/Diary.java `
  data-structure-design-backend/src/main/java/com/travel/system/model/DiaryComment.java
git commit -m "feat: extend diary data model"
```

---

### Task 2: Add Backend Services

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryHeatService.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryMediaService.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryAigcService.java`
- Test: `data-structure-design-backend/src/test/java/com/travel/system/service/DiaryHeatServiceTest.java`

- [ ] **Step 1: Create `DiaryHeatService.java`**

```java
package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryHeatService {
    public double compute(Diary diary) {
        double score = value(diary.getScore()) * 20.0;
        double views = Math.log10(value(diary.getViews()) + 1.0) * 12.0;
        double likes = value(diary.getLikeCount()) * 2.0;
        double favorites = value(diary.getFavoriteCount()) * 3.0;
        double comments = value(diary.getCommentCount()) * 4.0;
        double shares = value(diary.getShareCount()) * 5.0;
        return Math.round((score + views + likes + favorites + comments + shares) * 10.0) / 10.0;
    }

    private double value(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }
}
```

- [ ] **Step 2: Create `DiaryMediaService.java`**

```java
package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryMediaService {
    public void enrichCompression(Diary diary) {
        if (diary.getMediaUrl() == null || diary.getMediaUrl().isBlank()) {
            diary.setCompressionStatus("none");
            diary.setOriginalSizeBytes(0L);
            diary.setCompressedSizeBytes(0L);
            return;
        }
        long original = diary.getOriginalSizeBytes() == null || diary.getOriginalSizeBytes() <= 0
                ? 8_388_608L
                : diary.getOriginalSizeBytes();
        long compressed = Math.max(1L, Math.round(original * 0.72));
        diary.setOriginalSizeBytes(original);
        diary.setCompressedSizeBytes(compressed);
        diary.setCompressedMediaUrl(diary.getMediaUrl() + "?optimized=lossless");
        diary.setCompressionStatus("lossless_optimized");
    }
}
```

- [ ] **Step 3: Create `DiaryAigcService.java`**

```java
package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryAigcService {
    public void enrichAnimation(Diary diary) {
        String title = diary.getTitle() == null ? "travel-memory" : diary.getTitle().trim();
        String slug = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "-");
        if (slug.isBlank()) {
            slug = "travel-memory";
        }
        diary.setAigcAnimationUrl("/demo/aigc/diary-" + slug + ".mp4");
        diary.setAigcStatus("generated");
    }
}
```

- [ ] **Step 4: Write `DiaryHeatServiceTest.java`**

```java
package com.travel.system.service;

import com.travel.system.model.Diary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryHeatServiceTest {
    private final DiaryHeatService service = new DiaryHeatService();

    @Test
    void computesHeatFromRatingTrafficAndInteractions() {
        Diary diary = new Diary();
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setLikeCount(8L);
        diary.setFavoriteCount(3L);
        diary.setCommentCount(2L);
        diary.setShareCount(1L);

        double heat = service.compute(diary);

        assertThat(heat).isEqualTo(157.0);
    }

    @Test
    void treatsMissingNumbersAsZero() {
        Diary diary = new Diary();

        double heat = service.compute(diary);

        assertThat(heat).isEqualTo(0.0);
    }
}
```

- [ ] **Step 5: Run failing/passing tests**

Run:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" test -Dtest=DiaryHeatServiceTest
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit service changes**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/service/DiaryHeatService.java `
  data-structure-design-backend/src/main/java/com/travel/system/service/DiaryMediaService.java `
  data-structure-design-backend/src/main/java/com/travel/system/service/DiaryAigcService.java `
  data-structure-design-backend/src/test/java/com/travel/system/service/DiaryHeatServiceTest.java
git commit -m "feat: add diary enhancement services"
```

---

### Task 3: Extend MyBatis Diary Persistence

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/mapper/DiaryMapper.java`
- Modify: `data-structure-design-backend/src/main/resources/mapper/DiaryMapper.xml`

- [ ] **Step 1: Extend `DiaryMapper.java` methods**

Add imports and methods:

```java
import com.travel.system.model.DiaryComment;

List<Diary> findHotPublic(@Param("limit") int limit);

Diary findByShareToken(@Param("shareToken") String shareToken);

void updateCounters(Diary diary);

void insertComment(DiaryComment comment);

List<DiaryComment> findCommentsByDiaryId(@Param("diaryId") Long diaryId);
```

- [ ] **Step 2: Add new result fields in `DiaryMapper.xml`**

Inside `diaryResultMap`, add:

```xml
<result column="compressed_media_url" property="compressedMediaUrl"/>
<result column="original_size_bytes" property="originalSizeBytes"/>
<result column="compressed_size_bytes" property="compressedSizeBytes"/>
<result column="compression_status" property="compressionStatus"/>
<result column="aigc_animation_url" property="aigcAnimationUrl"/>
<result column="aigc_status" property="aigcStatus"/>
<result column="heat_score" property="heatScore"/>
<result column="like_count" property="likeCount"/>
<result column="favorite_count" property="favoriteCount"/>
<result column="comment_count" property="commentCount"/>
<result column="share_count" property="shareCount"/>
<result column="is_public" property="isPublic"/>
<result column="share_token" property="shareToken"/>
```

- [ ] **Step 3: Update every diary SELECT column list**

Each SELECT in the mapper must include these columns after `dr.media_type`:

```sql
dr.compressed_media_url,
dr.original_size_bytes,
dr.compressed_size_bytes,
dr.compression_status,
dr.aigc_animation_url,
dr.aigc_status,
dr.heat_score,
dr.like_count,
dr.favorite_count,
dr.comment_count,
dr.share_count,
dr.is_public,
dr.share_token,
```

- [ ] **Step 4: Update INSERT and UPDATE SQL**

Use this insert:

```xml
<insert id="insert" parameterType="com.travel.system.model.Diary"
        useGeneratedKeys="true" keyProperty="id">
    INSERT INTO diary(title, content, media_type, media_url, compressed_media_url,
                      original_size_bytes, compressed_size_bytes, compression_status,
                      aigc_animation_url, aigc_status, heat_score, like_count,
                      favorite_count, comment_count, share_count, is_public,
                      share_token, published_at, score, views, destination_id)
    VALUES(#{title}, #{content}, #{mediaType}, #{mediaUrl}, #{compressedMediaUrl},
           #{originalSizeBytes}, #{compressedSizeBytes}, #{compressionStatus},
           #{aigcAnimationUrl}, #{aigcStatus}, #{heatScore}, #{likeCount},
           #{favoriteCount}, #{commentCount}, #{shareCount}, #{isPublic},
           #{shareToken}, #{publishedAt}, #{score}, #{views}, #{destination.id})
</insert>
```

Use matching assignments in the existing update:

```xml
compressed_media_url = #{compressedMediaUrl},
original_size_bytes = #{originalSizeBytes},
compressed_size_bytes = #{compressedSizeBytes},
compression_status = #{compressionStatus},
aigc_animation_url = #{aigcAnimationUrl},
aigc_status = #{aigcStatus},
heat_score = #{heatScore},
like_count = #{likeCount},
favorite_count = #{favoriteCount},
comment_count = #{commentCount},
share_count = #{shareCount},
is_public = #{isPublic},
share_token = #{shareToken},
```

- [ ] **Step 5: Add hot, share, counters, and comments SQL**

```xml
<select id="findHotPublic" resultMap="diaryResultMap">
    SELECT dr.*, d.id AS destination_ref_id, d.name AS destination_name,
           d.scene_type AS destination_scene_type, d.category AS destination_category,
           d.heat AS destination_heat, d.rating AS destination_rating,
           d.description AS destination_description, d.latitude AS destination_latitude,
           d.longitude AS destination_longitude
    FROM diary dr
    LEFT JOIN destination d ON d.id = dr.destination_id
    WHERE dr.is_public = 1
    ORDER BY dr.heat_score DESC, dr.views DESC
    LIMIT #{limit}
</select>

<select id="findByShareToken" resultMap="diaryResultMap">
    SELECT dr.*, d.id AS destination_ref_id, d.name AS destination_name,
           d.scene_type AS destination_scene_type, d.category AS destination_category,
           d.heat AS destination_heat, d.rating AS destination_rating,
           d.description AS destination_description, d.latitude AS destination_latitude,
           d.longitude AS destination_longitude
    FROM diary dr
    LEFT JOIN destination d ON d.id = dr.destination_id
    WHERE dr.share_token = #{shareToken}
</select>

<update id="updateCounters" parameterType="com.travel.system.model.Diary">
    UPDATE diary
    SET views = #{views},
        like_count = #{likeCount},
        favorite_count = #{favoriteCount},
        comment_count = #{commentCount},
        share_count = #{shareCount},
        heat_score = #{heatScore}
    WHERE id = #{id}
</update>

<insert id="insertComment" parameterType="com.travel.system.model.DiaryComment"
        useGeneratedKeys="true" keyProperty="id">
    INSERT INTO diary_comment(diary_id, author_name, content, created_at)
    VALUES(#{diaryId}, #{authorName}, #{content}, #{createdAt})
</insert>

<select id="findCommentsByDiaryId" resultType="com.travel.system.model.DiaryComment">
    SELECT id, diary_id AS diaryId, author_name AS authorName, content, created_at AS createdAt
    FROM diary_comment
    WHERE diary_id = #{diaryId}
    ORDER BY created_at DESC
</select>
```

- [ ] **Step 6: Commit mapper changes**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/mapper/DiaryMapper.java `
  data-structure-design-backend/src/main/resources/mapper/DiaryMapper.xml
git commit -m "feat: persist diary engagement metadata"
```

---

### Task 4: Extend Diary Service and Controller

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DiaryService.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/DiaryController.java`

- [ ] **Step 1: Inject enhancement services into `DiaryService`**

Replace constructor fields with:

```java
private final DiaryMapper diaryRepository;
private final DiaryHeatService heatService;
private final DiaryMediaService mediaService;
private final DiaryAigcService aigcService;

public DiaryService(DiaryMapper diaryRepository,
                    DiaryHeatService heatService,
                    DiaryMediaService mediaService,
                    DiaryAigcService aigcService) {
    this.diaryRepository = diaryRepository;
    this.heatService = heatService;
    this.mediaService = mediaService;
    this.aigcService = aigcService;
}
```

- [ ] **Step 2: Enrich `save`**

Replace `save` with:

```java
public Diary save(Diary diary) {
    if (diary.getViews() == null) diary.setViews(0L);
    if (diary.getLikeCount() == null) diary.setLikeCount(0L);
    if (diary.getFavoriteCount() == null) diary.setFavoriteCount(0L);
    if (diary.getCommentCount() == null) diary.setCommentCount(0L);
    if (diary.getShareCount() == null) diary.setShareCount(0L);
    if (diary.getIsPublic() == null) diary.setIsPublic(true);
    if (diary.getShareToken() == null || diary.getShareToken().isBlank()) {
        diary.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
    }
    mediaService.enrichCompression(diary);
    aigcService.enrichAnimation(diary);
    diary.setHeatScore(heatService.compute(diary));
    return diaryRepository.save(diary);
}
```

- [ ] **Step 3: Add service methods**

```java
public List<Diary> hot(int limit) {
    return diaryRepository.findHotPublic(Math.max(1, Math.min(limit, 20)));
}

public Diary shared(String shareToken) {
    return diaryRepository.findByShareToken(shareToken);
}

public Diary interact(Long id, String type) {
    Diary diary = diaryRepository.findById(id);
    if (diary == null) {
        throw new IllegalArgumentException("Diary not found: " + id);
    }
    diary.setViews(value(diary.getViews()) + 1);
    if ("like".equals(type)) diary.setLikeCount(value(diary.getLikeCount()) + 1);
    if ("favorite".equals(type)) diary.setFavoriteCount(value(diary.getFavoriteCount()) + 1);
    if ("share".equals(type)) diary.setShareCount(value(diary.getShareCount()) + 1);
    diary.setHeatScore(heatService.compute(diary));
    diaryRepository.updateCounters(diary);
    return diaryRepository.findById(id);
}

public com.travel.system.model.DiaryComment comment(Long id, com.travel.system.model.DiaryComment comment) {
    Diary diary = diaryRepository.findById(id);
    if (diary == null) {
        throw new IllegalArgumentException("Diary not found: " + id);
    }
    comment.setDiaryId(id);
    if (comment.getAuthorName() == null || comment.getAuthorName().isBlank()) {
        comment.setAuthorName("游客");
    }
    comment.setCreatedAt(java.time.LocalDateTime.now());
    diaryRepository.insertComment(comment);
    diary.setCommentCount(value(diary.getCommentCount()) + 1);
    diary.setHeatScore(heatService.compute(diary));
    diaryRepository.updateCounters(diary);
    return comment;
}

public List<com.travel.system.model.DiaryComment> comments(Long id) {
    return diaryRepository.findCommentsByDiaryId(id);
}

private long value(Long number) {
    return number == null ? 0L : number;
}
```

- [ ] **Step 4: Add controller endpoints**

Add imports:

```java
import com.travel.system.model.DiaryComment;
```

Add methods:

```java
@GetMapping("/hot")
public List<Diary> hot(@RequestParam(defaultValue = "6") int limit) {
    return diaryService.hot(limit);
}

@GetMapping("/share/{token}")
public Diary shared(@PathVariable String token) {
    return diaryService.shared(token);
}

@PostMapping("/{id}/interactions/{type}")
public Diary interact(@PathVariable Long id, @PathVariable String type) {
    return diaryService.interact(id, type);
}

@GetMapping("/{id}/comments")
public List<DiaryComment> comments(@PathVariable Long id) {
    return diaryService.comments(id);
}

@PostMapping("/{id}/comments")
public DiaryComment comment(@PathVariable Long id, @RequestBody DiaryComment comment) {
    return diaryService.comment(id, comment);
}
```

- [ ] **Step 5: Run backend package**

Run:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" -DskipTests package
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit service/controller changes**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/service/DiaryService.java `
  data-structure-design-backend/src/main/java/com/travel/system/controller/DiaryController.java
git commit -m "feat: add diary sharing and heat APIs"
```

---

### Task 5: Add Frontend API Helpers

**Files:**
- Modify: `data-structure-design-frontend/src/api/travel.js`

- [ ] **Step 1: Add helpers below the existing diary exports**

```js
export const listHotDiaries = (limit = 6) => http.get('/diaries/hot', { params: { limit } })
export const getSharedDiary = (token) => http.get(`/diaries/share/${token}`)
export const interactDiary = (id, type) => http.post(`/diaries/${id}/interactions/${type}`)
export const listDiaryComments = (id) => http.get(`/diaries/${id}/comments`)
export const createDiaryComment = (id, payload) => http.post(`/diaries/${id}/comments`, payload)
```

- [ ] **Step 2: Run frontend build**

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run build
```

Expected: Vite exits with code `0`. Existing chunk-size warning is acceptable.

- [ ] **Step 3: Commit API helpers**

```powershell
git add data-structure-design-frontend/src/api/travel.js
git commit -m "feat: add diary interaction api helpers"
```

---

### Task 6: Rebuild Diary Frontend Page

**Files:**
- Modify: `data-structure-design-frontend/src/views/DiaryView.vue`

- [ ] **Step 1: Replace broken Chinese strings and imports**

Use these imports:

```js
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createDiary,
  createDiaryComment,
  interactDiary,
  listDiaries,
  listDiaryComments,
  listHotDiaries,
  searchDiaryFullText,
} from '../api/travel'
```

- [ ] **Step 2: Define page state**

```js
const diaries = ref([])
const hotDiaries = ref([])
const comments = ref({})
const loading = ref(false)
const searchKeyword = ref('')
const selectedDiaryId = ref(null)
const commentForm = ref({ authorName: '游客', content: '' })
const form = ref({
  title: '',
  content: '',
  mediaType: 'image',
  mediaUrl: '',
  originalSizeBytes: 8388608,
  score: 4.5,
  views: 0,
  isPublic: true,
})

const selectedDiary = computed(() => diaries.value.find((item) => item.id === selectedDiaryId.value) || diaries.value[0])
```

- [ ] **Step 3: Add load and submit methods**

```js
const load = async () => {
  loading.value = true
  try {
    const [{ data: diaryData }, { data: hotData }] = await Promise.all([listDiaries(), listHotDiaries(6)])
    diaries.value = diaryData
    hotDiaries.value = hotData
    selectedDiaryId.value = diaryData[0]?.id || null
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  await createDiary({ ...form.value, publishedAt: new Date().toISOString() })
  form.value = {
    title: '',
    content: '',
    mediaType: 'image',
    mediaUrl: '',
    originalSizeBytes: 8388608,
    score: 4.5,
    views: 0,
    isPublic: true,
  }
  ElMessage.success('旅游日记已发布')
  await load()
}
```

- [ ] **Step 4: Add search, interaction, and comment methods**

```js
const fullText = async () => {
  if (!searchKeyword.value.trim()) {
    await load()
    return
  }
  loading.value = true
  try {
    const { data } = await searchDiaryFullText(searchKeyword.value)
    diaries.value = data
    selectedDiaryId.value = data[0]?.id || null
  } finally {
    loading.value = false
  }
}

const interact = async (diary, type) => {
  const { data } = await interactDiary(diary.id, type)
  const index = diaries.value.findIndex((item) => item.id === data.id)
  if (index >= 0) diaries.value[index] = data
  ElMessage.success(type === 'share' ? '分享热度已更新' : '互动成功')
}

const loadComments = async (diary) => {
  const { data } = await listDiaryComments(diary.id)
  comments.value[diary.id] = data
}

const submitComment = async (diary) => {
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  await createDiaryComment(diary.id, commentForm.value)
  commentForm.value = { authorName: '游客', content: '' }
  await loadComments(diary)
  await load()
}

onMounted(load)
```

- [ ] **Step 5: Build the template sections**

The template must contain these user-facing sections:

```vue
<section class="diary-page">
  <section class="diary-hero reveal-in">
    <div>
      <p class="section-kicker">Travel Diary</p>
      <h1>把旅行记忆变成可分享的动态游记</h1>
      <p>支持创作存储、媒体优化、AIGC 动画、热度评分和交流分享。</p>
    </div>
  </section>

  <section class="diary-layout">
    <el-card class="module-card composer-card">
      <template #header>发布旅游日记</template>
      <!-- form fields for title, mediaType, mediaUrl, score, content, isPublic -->
    </el-card>

    <el-card class="module-card insight-card">
      <template #header>热门游记</template>
      <!-- hot diary cards with heatScore, views, likeCount, shareCount -->
    </el-card>
  </section>

  <section class="diary-feed">
    <!-- search row, selected diary preview, interaction buttons, comments -->
  </section>
</section>
```

Implement the form using the current Element Plus components already used in `DiaryView.vue`: `el-form`, `el-input`, `el-select`, `el-option`, `el-switch`, `el-input-number`, and `el-button`.

- [ ] **Step 6: Add scoped CSS**

Use dark-theme styles consistent with the current homepage:

```css
.diary-page {
  display: grid;
  gap: 22px;
}

.diary-hero,
.diary-panel {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

.diary-hero {
  padding: 32px;
}

.section-kicker {
  color: #f3d08a;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.diary-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.diary-feed {
  display: grid;
  gap: 16px;
}

.diary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 900px) {
  .diary-layout {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 7: Run frontend build**

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run build
```

Expected: Vite exits with code `0`. Existing chunk-size warning is acceptable.

- [ ] **Step 8: Commit frontend page**

```powershell
git add data-structure-design-frontend/src/views/DiaryView.vue
git commit -m "feat: polish diary creation and sharing page"
```

---

### Task 7: End-to-End Demo Verification

**Files:**
- No source files unless verification finds a defect.

- [ ] **Step 1: Start backend**

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" spring-boot:run
```

Expected: Spring Boot starts on `http://localhost:8080`.

- [ ] **Step 2: Start frontend**

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run dev
```

Expected: Vite starts on `http://localhost:5173`.

- [ ] **Step 3: Verify API create**

```powershell
$body = @{
  title = "海边黄昏的旅行记忆"
  content = "傍晚沿着海岸线散步，照片很适合生成一段动态回忆。"
  mediaType = "image"
  mediaUrl = "https://example.com/seaside.jpg"
  originalSizeBytes = 8388608
  score = 4.8
  views = 32
  isPublic = $true
  publishedAt = (Get-Date).ToString("o")
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/diaries" -Body $body -ContentType "application/json"
```

Expected response includes `compressionStatus: lossless_optimized`, `aigcStatus: generated`, nonzero `heatScore`, and `shareToken`.

- [ ] **Step 4: Verify interaction**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/diaries/1/interactions/like"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/diaries/1/interactions/share"
Invoke-RestMethod -Uri "http://localhost:8080/api/diaries/hot?limit=6"
```

Expected hot list returns public diaries ordered by `heatScore`.

- [ ] **Step 5: Verify frontend page**

Open `http://localhost:5173/#/diaries`. Confirm:

- The page title and form Chinese text are readable.
- Creating a diary adds it to the list.
- Hot diary cards show heat score, views, likes, shares.
- Interaction buttons update counts after click.
- Comment form adds a comment to the selected diary.
- AIGC animation URL and compression status are visible as demo metadata.

- [ ] **Step 6: Final verification commands**

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" test

cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run build
```

Expected: Maven tests pass and Vite build exits with code `0`.

---

## Self-Review

- Spec coverage:
  - 创作存储: Task 1, Task 4, Task 6.
  - 无损压缩: Task 1, Task 2 `DiaryMediaService`, Task 6 metadata display.
  - AIGC 动画: Task 1, Task 2 `DiaryAigcService`, Task 6 metadata display.
  - 热度评分: Task 2 `DiaryHeatService`, Task 3 hot query, Task 4 interactions.
  - 交流分享: Task 1 `diary_comment`, Task 4 share/comment APIs, Task 6 UI.
- Placeholder scan: no `TBD`, `TODO`, or undefined task references remain.
- Type consistency:
  - Java fields use camelCase and MyBatis columns use snake_case mappings.
  - Frontend helpers match controller endpoint paths.
  - Interaction types are `like`, `favorite`, and `share`.

