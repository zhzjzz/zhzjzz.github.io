# Amap Food Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the food module usable for nearby restaurant search with filters, distance, rating, and average price backed by imported commercial map POI data.

**Architecture:** Extend the existing Spring Boot/MyBatis food module and Vue food page. Add `average_price` to the food schema and API model, complete place-anchor lookup from route spots, and add an AMap Web Service importer that can fetch restaurant POIs and their `biz_ext.cost` values when an API key is available.

**Tech Stack:** Java 17, Spring Boot, MyBatis, SQLite/GeoPackage, Vue 3, Element Plus, Python standard library importer scripts.

---

### Task 1: Backend Food Search Contract

**Files:**
- Modify: `data-structure-design-backend/src/test/java/com/travel/system/service/FoodServiceTest.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/model/Food.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/FoodService.java`
- Modify: `data-structure-design-backend/src/main/resources/mapper/FoodMapper.xml`

- [ ] Add failing service tests for database place anchors, friendly cuisine labels, and average price sorting/filtering.
- [ ] Run `.\.tools\apache-maven-3.9.9\bin\mvn.cmd -Dmaven.repo.local=.m2 -f data-structure-design-backend\pom.xml -Dtest=FoodServiceTest test` and confirm the new tests fail.
- [ ] Implement `averagePrice`, friendly cuisine normalization, anchor lookup via `FoodMapper.findPlaceAnchors()`, and price sorting/filtering.
- [ ] Re-run the focused Maven test until it passes.

### Task 2: Schema And Data Import

**Files:**
- Modify: `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DataInitializer.java`
- Create: `data-structure-design-backend/scripts/import_amap_food.py`

- [ ] Add `average_price REAL` to the SQLite schema and startup migration.
- [ ] Backfill seed foods with deterministic average prices.
- [ ] Add an AMap importer using `AMAP_KEY`, `AMAP_FOOD_CITY`, and nearby spot anchors to fetch restaurant POIs, parse `biz_ext.cost`, and upsert rows as `source_type='amap'`.
- [ ] Keep imports idempotent by `source_type/source_id`.

### Task 3: Frontend Food Experience

**Files:**
- Modify: `data-structure-design-frontend/src/views/FoodView.vue`

- [ ] Add price range controls and average price display.
- [ ] Send `minAveragePrice`, `maxAveragePrice`, and price sort values to `/api/foods`.
- [ ] Keep distance, rating, cuisine, and place filters visible and responsive on mobile.
- [ ] Run `npm.cmd run build` in the frontend and inspect `/foods` in the browser.

### Task 4: Verification And Git

**Files:**
- Verify: backend tests, frontend build, git diff

- [ ] Run focused backend tests.
- [ ] Run frontend production build.
- [ ] Start or refresh the local app and browser-check the `/foods` page.
- [ ] Review `git status --short` and commit only related files on `codex/food-module`.
