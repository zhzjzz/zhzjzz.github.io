# Food Module Design

## Goal

Build a visitor-facing food module for the tourism system. The module focuses on search, recommendation, and browsing. It does not include admin CRUD screens.

## Current State

- Backend already has `FoodController`, `FoodService`, `FoodMapper`, and `Food` model.
- Frontend homepage already calls `/api/foods/top`, but there is no standalone food page.
- The local `food` table currently has only one row, and `destination` has no usable rows in the inspected database, so the module needs controlled demo seed data before it can be shown well.
- Existing local build artifacts and tool directories are untracked or modified and should not be included in food-module commits.

## Recommended Approach

Create a standalone `/foods` page and connect it from the top navigation and homepage quick actions. Keep the homepage food section as a lightweight preview, while the full visitor workflow lives on the dedicated page.

Use controlled seed data rather than a scraper for the first implementation. This is safer for a course project because it avoids API keys, anti-scraping issues, unstable page formats, and unclear data rights. A later import script can be added if real POI data is required.

## Backend Design

Extend the existing food APIs instead of creating a parallel module.

Endpoints:

- `GET /api/foods`
  - Query params: `keyword`, `cuisine`, `destinationId`, `sort`, `limit`.
  - `keyword` matches food name, cuisine, or store name.
  - `sort` supports `recommend`, `rating`, and `destinationHeat`.
  - The response is a list of food records with destination summary fields.
- `GET /api/foods/top`
  - Keeps existing behavior for homepage compatibility.
  - Supports optional `k` and uses the same recommendation scoring.

Data model:

- Keep existing fields: `id`, `name`, `cuisine`, `storeName`, `rating`, `destination`.
- Add or expose `heat` if the database and mapper need it for stronger recommendation scoring.
- Use destination heat as a fallback signal when food heat is absent.

Recommendation logic:

- Use a bounded min-heap for Top-K recommendations.
- Score food by normalized rating plus heat signal.
- Keep invalid `k` values bounded to a safe default.

Seed data:

- Ensure several demo destinations exist.
- Insert 30-50 food rows across campus and tourism scenes.
- Make seed insertion idempotent by checking existing food count or unique names before inserting.

## Frontend Design

Add `FoodView.vue` under `src/views`.

Page capabilities:

- Search by food name, cuisine, or store name.
- Filter by cuisine.
- Filter by destination.
- Sort by recommendation, rating, or destination heat.
- Show food cards with image, name, cuisine, store, rating, and destination.
- Show loading, empty, and error states.

Navigation:

- Add `/foods` route.
- Add "美食" to `AppNav.vue`.
- Change quick action "吃什么" to open `/foods`.
- Keep homepage food preview and link it to the dedicated page.

Visual style:

- Follow the existing dark travel-dashboard style.
- Use the existing food default image asset.
- Avoid nested cards and keep the page scannable on desktop and mobile.

## Error Handling

- Backend returns an empty list for no matches.
- Backend sanitizes blank keyword and invalid limit/sort values.
- Frontend handles API failure with a concise error message and keeps the page usable.
- If no cuisine or destination filters are available, the page still supports keyword search and recommendation browsing.

## Testing And Verification

Backend:

- Add or update service tests for keyword filtering, sorting, Top-K bounds, and seed-data-safe behavior.
- Run Maven tests.

Frontend:

- Run `npm run build`.
- Start the dev server and inspect `/foods`.
- Check desktop and mobile layout for text overflow and broken cards.

Git checkpoints:

1. Commit this design spec.
2. Commit backend API, seed data, and tests.
3. Commit frontend page, routes, and navigation.
4. Commit final polish or deployment build artifacts only if explicitly needed.
