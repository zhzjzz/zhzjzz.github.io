# Demo Frontend Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the travel system frontend for a teacher-facing demo by making the UI clearer, more polished, and easier to present.

**Architecture:** Keep the existing Vue 3 + Vite + Element Plus app. Add a small shared presentation layer in global CSS, then polish the navigation, home dashboard, destination ranking page, and route-planning demo flow without introducing new libraries or backend changes.

**Tech Stack:** Vue 3, Vite 8, Element Plus, Pinia, Vue Router, AMap JS API, existing REST API client.

---

## Scope

This plan implements the agreed direction: "refined product UI + demo narrative." It does not redesign the backend, database, authentication flow, or route algorithm. It keeps all dependencies unchanged and uses the existing `npm.cmd run build` verification path.

## File Structure

- Modify: `data-structure-design-frontend/src/style.css`
  - Shared visual tokens, demo cards, metric cards, reveal animations, empty/loading panels, responsive helpers.
- Modify: `data-structure-design-frontend/src/App.vue`
  - Subtle app background and page spacing tuned for demo pages.
- Modify: `data-structure-design-frontend/src/components/AppNav.vue`
  - Clear Chinese navigation labels, stronger brand area, "演示路线" shortcut.
- Modify: `data-structure-design-frontend/src/views/HomeView.vue`
  - Convert the home page into a demo dashboard and presentation entry.
- Modify: `data-structure-design-frontend/src/views/DestinationView.vue`
  - Replace plain table-first layout with leaderboard cards plus supporting table.
- Modify: `data-structure-design-frontend/src/views/RouteView.vue`
  - Add a "演示模式" quick-fill action, stronger map/result composition, and clearer route timeline.
- Modify: `data-structure-design-frontend/src/views/FacilityView.vue`
  - Light polish only: clearer labels, result summary, better empty state.
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`
  - Light polish only: clearer labels and demo-ready copy.

## Visual Principles

- Use a restrained travel-tech palette: coral red for primary actions, ink text, teal/amber/blue accents for data and route segments.
- Keep cards professional: maximum border radius around 20px, avoid nested cards, avoid decorative orbs.
- Every demo screen must answer: what does this module do, what data is visible, and what should the presenter click next?
- Use motion only for state transitions and entrance polish; keep durations around 180-260ms and respect reduced motion.

---

### Task 1: Shared Demo Visual System

**Files:**
- Modify: `data-structure-design-frontend/src/style.css`
- Modify: `data-structure-design-frontend/src/App.vue`

- [ ] **Step 1: Add shared CSS tokens and reusable classes**

Append these blocks after the existing `:root` variables and before the component selectors in `src/style.css`. Keep existing Element Plus overrides.

```css
:root {
  --demo-ink: #111827;
  --demo-muted: #64748b;
  --demo-coral: #ff385c;
  --demo-coral-dark: #d90f3f;
  --demo-teal: #0f766e;
  --demo-amber: #d97706;
  --demo-blue: #2563eb;
  --demo-surface-soft: #f8fafc;
  --demo-border: rgba(15, 23, 42, 0.08);
  --demo-shadow-soft: 0 18px 48px rgba(15, 23, 42, 0.08);
  --demo-shadow-lift: 0 24px 70px rgba(15, 23, 42, 0.14);
}

.demo-hero {
  display: grid;
  gap: 18px;
  padding: 28px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(255, 56, 92, 0.10), rgba(255, 255, 255, 0.96) 42%),
    linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid rgba(255, 56, 92, 0.14);
  box-shadow: var(--demo-shadow-soft);
}

.demo-eyebrow {
  color: var(--demo-coral);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.demo-title {
  color: var(--demo-ink);
  font-size: clamp(28px, 3vw, 44px);
  line-height: 1.12;
  font-weight: 900;
}

.demo-copy {
  max-width: 760px;
  color: var(--demo-muted);
  font-size: 15px;
  line-height: 1.7;
  font-weight: 400;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.metric-card {
  min-height: 116px;
  padding: 18px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid var(--demo-border);
  box-shadow: var(--demo-shadow-soft);
}

.metric-card span {
  display: block;
  color: var(--demo-muted);
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  color: var(--demo-ink);
  font-size: 30px;
  line-height: 1;
  font-weight: 900;
}

.status-panel {
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px dashed rgba(15, 23, 42, 0.16);
  color: var(--demo-muted);
}

.reveal-in {
  animation: demoReveal 220ms ease-out both;
}

@keyframes demoReveal {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .reveal-in {
    animation: none;
  }
}
```

- [ ] **Step 2: Tune the app background**

In `src/App.vue`, replace the `.app-shell` background with:

```css
.app-shell {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(248, 250, 252, 0.96) 48%, #ffffff 100%);
}
```

Keep `.page-container`, `.login-shell`, and media queries.

- [ ] **Step 3: Run build verification**

Run:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run build
```

Expected: `✓ built` and exit code `0`.

- [ ] **Step 4: Commit**

```powershell
git add data-structure-design-frontend/src/style.css data-structure-design-frontend/src/App.vue
git commit -m "style: add demo visual system"
```

---

### Task 2: Navigation And Presentation Entry

**Files:**
- Modify: `data-structure-design-frontend/src/components/AppNav.vue`

- [ ] **Step 1: Replace navigation labels with clean Chinese copy**

Use this `navItems` array:

```js
const navItems = [
  { label: '总览', path: '/' },
  { label: '目的地推荐', path: '/destinations' },
  { label: '路线规划', path: '/routes' },
  { label: '场所查询', path: '/facilities' },
  { label: '旅行日记', path: '/diaries' },
  { label: '协作行程', path: '/itineraries' },
]
```

Update accessible labels:

```vue
<button class="brand" type="button" aria-label="返回总览" @click="go('/')">
```

```vue
<button class="search-pill" type="button" aria-label="进入目的地推荐" @click="go('/destinations')">
```

- [ ] **Step 2: Make the nav support a route demo shortcut**

Replace the existing `host-link` button text and route:

```vue
<button class="host-link" type="button" @click="go('/routes')">演示路线</button>
```

Replace brand text:

```vue
<strong>Travel.AI</strong>
<small>个性化旅行系统</small>
```

Replace search pill text:

```vue
<span>景区</span>
<span>校园</span>
<span>路线</span>
```

Replace user fallback and logout:

```vue
<span class="user-name">{{ appStore.user.name || '未登录' }}</span>
<button class="logout-btn" type="button" @click="logout">退出</button>
```

- [ ] **Step 3: Refine nav styling**

In the same file, update `.brand-mark` and `.category-pill.active`:

```css
.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff385c, #d90f3f);
  color: #ffffff;
  font-weight: 900;
  box-shadow: rgba(255, 56, 92, 0.26) 0 12px 28px;
}

.category-pill.active {
  color: #111827;
  border-bottom-color: #ff385c;
}
```

- [ ] **Step 4: Verify in browser**

Run frontend dev server if needed:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run dev
```

Open `http://localhost:5173/#/destinations`.
Expected: no visible garbled nav text, active tab underline is coral red, "演示路线" goes to `#/routes`.

- [ ] **Step 5: Commit**

```powershell
git add data-structure-design-frontend/src/components/AppNav.vue
git commit -m "style: polish demo navigation"
```

---

### Task 3: Home Page Demo Dashboard

**Files:**
- Modify: `data-structure-design-frontend/src/views/HomeView.vue`

- [ ] **Step 1: Add dashboard metadata**

After `const router = useRouter()`, add:

```js
const featureCards = [
  { title: '目的地推荐', text: '按评分、热度和综合分生成 Top10 推荐。', path: '/destinations', accent: 'coral' },
  { title: '多景区路线', text: '串联多个景区和地点，展示总距离、总耗时与分段路径。', path: '/routes', accent: 'teal' },
  { title: '附近场所', text: '从目的地坐标出发，查询周边服务设施。', path: '/facilities', accent: 'amber' },
  { title: '协作行程', text: '管理多人行程、策略、交通方式与备注。', path: '/itineraries', accent: 'blue' },
]

const dashboardStats = [
  { label: '推荐模块', value: '4' },
  { label: '演示路径', value: '1' },
  { label: '路线类型', value: '多景区' },
  { label: '数据源', value: 'SQLite' },
]

const go = (path) => {
  router.push(path)
}
```

- [ ] **Step 2: Replace the current template**

Use this template structure:

```vue
<template>
  <section class="home-page">
    <section class="demo-hero home-hero reveal-in">
      <div>
        <p class="demo-eyebrow">Travel AI Showcase</p>
        <h1 class="demo-title">个性化旅行推荐与路线规划系统</h1>
        <p class="demo-copy">
          面向课堂演示的完整旅行系统：从目的地推荐、场所查询到多景区路线规划和协作行程管理，形成一条清晰的功能闭环。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="go('/routes')">进入路线演示</el-button>
        <el-button size="large" @click="go('/destinations')">查看推荐榜单</el-button>
      </div>
    </section>

    <section class="demo-grid">
      <article v-for="item in dashboardStats" :key="item.label" class="metric-card reveal-in">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="feature-grid">
      <button
        v-for="item in featureCards"
        :key="item.title"
        type="button"
        class="feature-card reveal-in"
        :class="`accent-${item.accent}`"
        @click="go(item.path)"
      >
        <span>{{ item.title }}</span>
        <strong>{{ item.text }}</strong>
      </button>
    </section>

    <section class="food-strip">
      <div class="module-header">
        <div>
          <h2>美食推荐样例</h2>
          <p class="module-subtitle">用于展示系统可以扩展到餐饮、景点、路线等多类旅行数据。</p>
        </div>
      </div>
      <div class="listings">
        <article class="listing-card reveal-in" v-for="item in foods" :key="item.id">
          <div class="listing-media">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
            <div v-else class="image-placeholder">暂无图片</div>
          </div>
          <div class="listing-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.cuisine || '地方特色' }} · 评分 {{ item.rating || '-' }}</p>
            <strong>精选推荐</strong>
          </div>
        </article>
        <el-empty v-if="!foods.length" description="暂无推荐数据" />
      </div>
    </section>
  </section>
</template>
```

- [ ] **Step 3: Replace the page-specific style**

Use the existing listing styles and add:

```css
.home-page {
  display: grid;
  gap: 24px;
}

.home-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.feature-card {
  min-height: 150px;
  padding: 20px;
  text-align: left;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.14);
}

.feature-card span {
  color: #ff385c;
  font-size: 13px;
  font-weight: 900;
}

.feature-card strong {
  display: block;
  margin-top: 12px;
  color: #111827;
  font-size: 18px;
  line-height: 1.5;
}

.accent-teal span { color: #0f766e; }
.accent-amber span { color: #d97706; }
.accent-blue span { color: #2563eb; }

.food-strip {
  display: grid;
  gap: 16px;
}

@media (max-width: 744px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
```

- [ ] **Step 4: Verify**

Run:

```powershell
npm.cmd run build
```

Expected: build succeeds. Browser check `http://localhost:5173/#/` and confirm the first screen explains the system and has clear route/recommendation entry buttons.

- [ ] **Step 5: Commit**

```powershell
git add data-structure-design-frontend/src/views/HomeView.vue
git commit -m "feat: add demo dashboard home"
```

---

### Task 4: Destination Recommendation Leaderboard

**Files:**
- Modify: `data-structure-design-frontend/src/views/DestinationView.vue`

- [ ] **Step 1: Add ranking helpers**

Update import:

```js
import { computed, onMounted, ref } from 'vue'
```

Add after `const rankMode = ref('composite')`:

```js
const topRows = computed(() => rows.value.slice(0, 10))
const featured = computed(() => topRows.value[0] || null)
const maxHeat = computed(() => Math.max(...topRows.value.map((item) => Number(item.heat) || 0), 1))

const modeLabel = computed(() => {
  const labels = {
    composite: '综合推荐',
    rating: '评分 Top10',
    heat: '热度 Top10',
  }
  return labels[rankMode.value] || '综合推荐'
})

const heatPercent = (item) => Math.round(((Number(item.heat) || 0) / maxHeat.value) * 100)
const ratingPercent = (item) => Math.min(100, Math.round(((Number(item.rating) || 0) / 5) * 100))
```

- [ ] **Step 2: Replace the result area**

Keep the toolbar. Replace the `el-table` block with:

```vue
<section v-if="featured" class="leaderboard-layout reveal-in">
  <article class="featured-destination">
    <span class="rank-badge">Top 1</span>
    <h3>{{ featured.name }}</h3>
    <p>{{ featured.sceneType || '旅行目的地' }} · {{ featured.category || '综合推荐' }}</p>
    <div class="score-row">
      <span>热度 {{ featured.heat || 0 }}</span>
      <span>评分 {{ featured.rating || '-' }}</span>
      <span>{{ modeLabel }}</span>
    </div>
  </article>

  <div class="ranking-list">
    <article v-for="(item, index) in topRows" :key="item.id || item.name" class="ranking-item">
      <div class="ranking-index">{{ index + 1 }}</div>
      <div class="ranking-main">
        <strong>{{ item.name }}</strong>
        <span>{{ item.sceneType || '目的地' }} · {{ item.category || '未分类' }}</span>
        <div class="bar-line">
          <i :style="{ width: `${heatPercent(item)}%` }" />
        </div>
      </div>
      <div class="ranking-score">
        <span>热度 {{ item.heat || 0 }}</span>
        <span>评分 {{ item.rating || '-' }}</span>
      </div>
    </article>
  </div>
</section>

<el-empty v-else-if="!loading" description="暂无推荐数据，请检查后端服务或搜索条件" />

<el-table :data="rows" stripe border v-loading="loading" class="detail-table">
  <el-table-column prop="name" label="名称" min-width="180" />
  <el-table-column prop="sceneType" label="场景" width="120" />
  <el-table-column prop="category" label="类别" width="160" />
  <el-table-column prop="heat" label="热度" width="120" />
  <el-table-column prop="rating" label="评分" width="120" />
</el-table>
```

- [ ] **Step 3: Add leaderboard CSS**

Append to the scoped style:

```css
.leaderboard-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
  margin-bottom: 18px;
}

.featured-destination {
  min-height: 260px;
  padding: 24px;
  border-radius: 22px;
  color: #ffffff;
  background: linear-gradient(135deg, #111827 0%, #ff385c 100%);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
}

.rank-badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 900;
}

.featured-destination h3 {
  margin-top: 22px;
  font-size: 30px;
  line-height: 1.18;
  font-weight: 900;
}

.featured-destination p {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 500;
}

.score-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}

.score-row span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.ranking-list {
  display: grid;
  gap: 10px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.ranking-index {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #fff1f4;
  color: #ff385c;
  font-weight: 900;
}

.ranking-main strong,
.ranking-main span,
.ranking-score span {
  display: block;
}

.ranking-main strong {
  color: #111827;
  font-size: 16px;
}

.ranking-main span,
.ranking-score span {
  color: #64748b;
  font-size: 12px;
}

.bar-line {
  height: 6px;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.bar-line i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff385c, #d97706);
}

.detail-table {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .leaderboard-layout,
  .ranking-item {
    grid-template-columns: 1fr;
  }

  .ranking-score {
    display: flex;
    gap: 12px;
  }
}
```

- [ ] **Step 4: Verify**

Run:

```powershell
npm.cmd run build
```

Browser check `http://localhost:5173/#/destinations`.
Expected: top destination is visually highlighted, Top10 list appears above the detail table, search still reloads results.

- [ ] **Step 5: Commit**

```powershell
git add data-structure-design-frontend/src/views/DestinationView.vue
git commit -m "feat: add destination leaderboard"
```

---

### Task 5: Route Planning Demo Mode

**Files:**
- Modify: `data-structure-design-frontend/src/views/RouteView.vue`

- [ ] **Step 1: Add demo loading state and scenario keywords**

After `const loading = ref(false)`, add:

```js
const demoLoading = ref(false)
const demoSpotKeywords = ['西湖', '故宫', '公园', '大学']
```

- [ ] **Step 2: Add helper functions before `addVisit`**

Insert these functions before `const addVisit = () => {`:

```js
const findDemoSpots = async () => {
  const selected = []
  for (const keyword of demoSpotKeywords) {
    const { data } = await searchNavSpots({ keyword, limit: 4 })
    for (const spot of data || []) {
      if (spot?.name && !selected.some((item) => item.name === spot.name)) {
        selected.push(spot)
      }
      if (selected.length >= 2) return selected
    }
  }
  return selected
}

const chooseDemoNodeIds = (visit) => {
  const options = nodeOptions(visit)
  return options.slice(0, 2).map((item) => item.value).filter((value) => value != null)
}

const applyDemoScenario = async () => {
  demoLoading.value = true
  routeResult.value = null
  clearPolylines()
  clearMarkers()
  try {
    const spots = await findDemoSpots()
    if (spots.length < 1) {
      ElMessage.warning('没有找到可用于演示的景区数据，请手动搜索景区')
      return
    }
    visits.value = spots.slice(0, 2).map((spot, index) => ({
      ...newVisit(),
      spotInput: spot.name,
      spot,
      transportMode: index === 0 ? 'walk' : 'bike',
    }))
    for (const visit of visits.value) {
      await loadVisitData(visit)
      visit.nodeIds = chooseDemoNodeIds(visit)
    }
    strategy.value = 'SHORTEST_TIME'
    optimizeVisitOrder.value = true
    drawSelectedMarkers()
    refreshMapView()
    ElMessage.success('演示路线参数已填充，可以直接点击规划路线')
  } catch (error) {
    console.error(error)
    ElMessage.error('演示数据填充失败，请检查后端服务和数据库')
  } finally {
    demoLoading.value = false
  }
}
```

- [ ] **Step 3: Add the demo button**

In `.toolbar-actions`, add a button before "添加景区":

```vue
<el-button :loading="demoLoading" @click="applyDemoScenario">演示模式</el-button>
```

- [ ] **Step 4: Upgrade result summary**

Replace the current `el-descriptions` result block with:

```vue
<section v-if="routeResult" class="route-summary-grid reveal-in">
  <article class="metric-card">
    <span>总距离</span>
    <strong>{{ totalDistanceKm }} km</strong>
  </article>
  <article class="metric-card">
    <span>总耗时</span>
    <strong>{{ totalDurationMin }} 分钟</strong>
  </article>
  <article class="metric-card">
    <span>路线段数</span>
    <strong>{{ routeResult.segments?.length || 0 }}</strong>
  </article>
</section>
```

- [ ] **Step 5: Add route summary CSS**

Append before media query:

```css
.route-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.segment-item {
  position: relative;
  overflow: hidden;
}

.segment-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #ff385c, #0f766e);
}

@media (max-width: 768px) {
  .route-summary-grid {
    grid-template-columns: 1fr;
  }
}
```

If the file already has an `@media (max-width: 768px)` block, merge the `.route-summary-grid` rule into it instead of creating a duplicate.

- [ ] **Step 6: Verify demo mode**

Run:

```powershell
npm.cmd run build
```

Run backend and frontend:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" spring-boot:run
```

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run dev
```

Browser check `http://localhost:5173/#/routes`.
Expected: clicking "演示模式" fills at least one scenic spot and its node choices; if two spots exist, both rows fill. Clicking "规划路线" shows summary cards and route segments.

- [ ] **Step 7: Commit**

```powershell
git add data-structure-design-frontend/src/views/RouteView.vue
git commit -m "feat: add route demo mode"
```

---

### Task 6: Facility And Itinerary Demo Polish

**Files:**
- Modify: `data-structure-design-frontend/src/views/FacilityView.vue`
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`

- [ ] **Step 1: Polish facility page visible copy**

In `FacilityView.vue`, use these text replacements in the template and messages:

```js
ElMessage.error('加载设施类别失败，请稍后重试')
ElMessage.warning('请先选择有坐标的起点目的地')
ElMessage.success(`已找到 ${data.length} 个附近场所`)
```

Use these labels:

```vue
<h2>场所查询</h2>
<p class="module-subtitle">从起点目的地出发，按空间距离查询周边服务设施，并支持类别过滤和关键词检索。</p>
<el-form-item label="起点目的地">
<el-form-item label="设施类别">
<el-form-item label="关键词">
<el-form-item label="最大距离（米）">
<el-button type="primary" :loading="loading" @click="search">查询附近场所</el-button>
```

- [ ] **Step 2: Add facility result summary**

Add this before the divider:

```vue
<div class="result-summary">
  <span>当前起点：{{ selectedDestinationName }}</span>
  <strong>{{ results.length }} 个结果</strong>
</div>
```

Append CSS:

```css
.result-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
}

.result-summary strong {
  color: #111827;
  font-size: 18px;
}
```

- [ ] **Step 3: Polish itinerary page visible copy**

In `ItineraryView.vue`, replace visible garbled strings with clear Chinese equivalents:

```vue
<div class="eyebrow">协作行程</div>
<h2>把路线、策略、备注和协作信息统一管理</h2>
<p>支持快速检索、查看详情、复制摘要和在线编辑。</p>
<el-button type="primary" size="large" @click="openCreate">新建行程</el-button>
<el-button size="large" @click="refresh">刷新</el-button>
```

Use these message strings:

```js
ElMessage.warning('请输入行程名称')
ElMessage.success('行程已更新')
ElMessage.success('行程已创建')
ElMessage.success('已复制摘要')
```

- [ ] **Step 4: Verify**

Run:

```powershell
npm.cmd run build
```

Browser check:
- `http://localhost:5173/#/facilities`
- `http://localhost:5173/#/itineraries`

Expected: labels and messages are readable Chinese, result summary appears on facilities, itinerary hero reads like a product module.

- [ ] **Step 5: Commit**

```powershell
git add data-structure-design-frontend/src/views/FacilityView.vue data-structure-design-frontend/src/views/ItineraryView.vue
git commit -m "style: polish supporting demo pages"
```

---

### Task 7: Final QA And Demo Script

**Files:**
- Create: `docs/demo-runbook.md`

- [ ] **Step 1: Create demo runbook**

Create `docs/demo-runbook.md` with:

````markdown
# Demo Runbook

## Start Backend

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" spring-boot:run
```

## Start Frontend

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run dev
```

## Presentation Flow

1. Open `http://localhost:5173/#/`.
2. Explain the system goal from the dashboard hero.
3. Open `目的地推荐` and show the Top10 leaderboard.
4. Open `路线规划`, click `演示模式`, then click `规划路线`.
5. Show map path, total distance, total time, and segment list.
6. Open `场所查询` to show nearby service search.
7. Open `协作行程` to show multi-person itinerary management.

## Backup Checks

- If the map does not load, verify `.env` has `VITE_AMAP_KEY` and `VITE_AMAP_SECRET`.
- If API calls fail, verify backend is running at `http://localhost:8080`.
- If route demo finds no data, search and select a scenic spot manually.
````

- [ ] **Step 2: Full build verification**

Run:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-frontend"
npm.cmd run build
```

Expected: `✓ built` and exit code `0`.

- [ ] **Step 3: Backend package verification**

Run:

```powershell
cd "C:\Users\stay g\Documents\New project\zhzjzz.github.io\data-structure-design-backend"
$env:JAVA_HOME="D:\software\jdk-26"
$env:Path="$env:JAVA_HOME\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;$env:Path"
mvn.cmd "-Dmaven.repo.local=D:\codex-deps\zhzjzz.github.io\m2" "-DskipTests" package
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Browser QA checklist**

Check these URLs:

```text
http://localhost:5173/#/
http://localhost:5173/#/destinations
http://localhost:5173/#/routes
http://localhost:5173/#/facilities
http://localhost:5173/#/itineraries
```

Expected:
- No visible garbled Chinese text in nav, headers, buttons, empty states, and primary messages.
- No horizontal scroll at desktop width and mobile width.
- Main buttons have visible loading or disabled state during async work.
- Destination leaderboard still responds to rank mode changes.
- Route page "演示模式" does not crash when no map key is configured; it still fills API-driven form data when backend data exists.

- [ ] **Step 5: Commit final docs**

```powershell
git add docs/demo-runbook.md
git commit -m "docs: add demo runbook"
```

---

## Self-Review

- Spec coverage: This plan covers the approved refined product UI, demo narrative, destination leaderboard, route demo mode, and supporting page polish.
- Placeholder scan: No task contains open-ended placeholders; each code-changing step includes concrete code or concrete text replacements.
- Type consistency: New identifiers are `featureCards`, `dashboardStats`, `go`, `topRows`, `featured`, `maxHeat`, `modeLabel`, `heatPercent`, `ratingPercent`, `demoLoading`, `demoSpotKeywords`, `findDemoSpots`, `chooseDemoNodeIds`, and `applyDemoScenario`; they are only used in the files where they are introduced.
