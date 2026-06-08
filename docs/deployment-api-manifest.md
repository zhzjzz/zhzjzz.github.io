# Deployment API Manifest

This file groups the APIs and upstream dependencies that need to be available when deploying this project.

Single-file deployment template:
- `deploy.api.env.example`
- use it as the master list for AMap, LLM, AIGC, and related runtime variables
- then copy the needed values into frontend/backend `.env` files during deployment

Base rule:
- Frontend calls backend with `/api/...`
- Backend serves local business APIs and may proxy to third-party APIs

## 1. Required backend APIs

### Auth
- `POST /api/auth/login`
- `POST /api/auth/register`

### Destinations
- `GET /api/destinations`
  - used by destination page and facility page
  - query params: `keyword`, `sort`
- `GET /api/destinations/top`
  - query params: `k`, `mode`, `interest`
- `GET /api/destinations/route-search`
  - query params: `keyword`, `limit`
- `POST /api/destinations`

### Foods
- `GET /api/foods`
  - query params may include:
  - `keyword`, `cuisine`, `destinationId`, `sort`, `limit`
  - `place`, `latitude`, `longitude`, `radiusMeters`
  - `minAveragePrice`, `maxAveragePrice`
- `GET /api/foods/top`
- `GET /api/foods/cuisines`
- `GET /api/foods/place-anchors`
- `GET /api/foods/amap`

### Diaries
- `GET /api/diaries`
- `POST /api/diaries`
- `GET /api/diaries/{id}`
- `DELETE /api/diaries/{id}`
- `GET /api/diaries/search`
- `GET /api/diaries/by-destination`
- `GET /api/diaries/exact-title`
- `GET /api/diaries/hot`
- `GET /api/diaries/share/{token}`
- `POST /api/diaries/{id}/interactions/{type}`
- `POST /api/diaries/{id}/rating`
- `POST /api/diaries/{id}/aigc-image`
- `GET /api/diaries/{id}/comments`
- `POST /api/diaries/{id}/comments`

### Facilities
- `GET /api/facilities`
- `GET /api/facilities/types`
- `GET /api/facilities/nearby`
  - query params may include:
  - `fromLat`, `fromLon`, `fromNodeId`
  - `type`, `keyword`, `maxDistanceMeters`
  - `spotName`, `sceneType`

### Itineraries
- `GET /api/itineraries`
- `POST /api/itineraries`
- `GET /api/itineraries/{id}`
- `PUT /api/itineraries/{id}`
- `DELETE /api/itineraries/{id}`

### Itinerary collaboration
- `GET /api/itineraries/{itineraryId}/spot-votes`
- `POST /api/itineraries/{itineraryId}/spot-votes`
- `GET /api/itineraries/{itineraryId}/map-spots`
- `POST /api/itineraries/{itineraryId}/map-spots`
- `POST /api/itinerary-planner/preview`
- `POST /api/itinerary-import/preview`
- `POST /api/itinerary-import/create`

### Travel agent
- `POST /api/agent/chat`
- `GET /api/agent/status`

### Navigation
- `GET /api/nav/spots`
- `GET /api/nav/spots/{spotId}`
- `GET /api/nav/buildings/by-spot`
- `GET /api/nav/pois/by-spot`
- `POST /api/nav/route/plan`
- `POST /api/nav/route/cross-spot`
- `POST /api/nav/route/multi-spot`
- `GET /api/nav/route/nodes`
- `GET /api/nav/route/edges`
- `POST /api/nav/indoor/plan`

## 2. Optional backend APIs not currently called by frontend but still exposed

### Buildings
- `GET /api/nav/buildings`
- `GET /api/nav/buildings/{buildingId}`
- `GET /api/nav/buildings/nearby-node`

### POIs
- `GET /api/nav/pois`
- `GET /api/nav/pois/{poiId}`
- `GET /api/nav/pois/nearby-node`

### Spots
- `GET /api/nav/spots/all`

## 3. Third-party APIs and external services

These are not frontend-direct deployment endpoints, but the deployment environment may need them.

### LLM service
- used by: `POST /api/agent/chat`
- env vars in backend:
  - `LLM_BASE_URL`
  - `LLM_API_KEY`
  - `LLM_MODEL`
  - `LLM_TEMPERATURE`
  - `LLM_TIMEOUT_SECONDS`

### AIGC image API
- used by: `POST /api/diaries/{id}/aigc-image`
- env vars in backend:
  - `AIGC_IMAGE_API_URL`
  - `AIGC_IMAGE_API_KEY`
  - `AIGC_IMAGE_MODEL`
- note:
  - if not configured, backend falls back to demo image output

### AMap food search
- used by: `GET /api/foods/amap`
- backend may need AMap credentials depending on current service configuration

### Frontend AMap JS SDK
- used by route and food map features in browser
- frontend env vars:
  - `VITE_AMAP_KEY`
  - `VITE_AMAP_SECRET`

## 4. Deployment minimum set

If you only want the APIs needed by the currently visible product flows, deploy these first:

- `/api/auth/*`
- `/api/destinations/*`
- `/api/foods/*`
- `/api/diaries/*`
- `/api/facilities/*`
- `/api/itineraries/*`
- `/api/itinerary-planner/preview`
- `/api/itinerary-import/*`
- `/api/agent/*`
- `/api/nav/spots*`
- `/api/nav/buildings/by-spot`
- `/api/nav/pois/by-spot`
- `/api/nav/route/*`
- `/api/nav/indoor/plan`

## 5. Suggested deployment checklist

1. Confirm backend exposes `/api/...` routes under the final domain.
2. Confirm frontend proxy or base URL points to the deployed backend.
3. Confirm SQLite or GeoPackage data file path is valid in production.
4. Confirm `LLM_*` variables are set if chat is required.
5. Confirm `AIGC_IMAGE_*` variables are set if real image generation is required.
6. Confirm `VITE_AMAP_KEY` and `VITE_AMAP_SECRET` are set for frontend map features.
7. Smoke test these pages after deployment:
   - `/login`
   - `/destinations`
   - `/foods`
   - `/diaries`
   - `/facilities`
   - `/routes`
   - `/itineraries`
