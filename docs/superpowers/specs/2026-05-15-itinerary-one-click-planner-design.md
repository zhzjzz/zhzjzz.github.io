# Itinerary One-Click Planner Design

## Goal

Add a reusable one-click itinerary planning module that turns collaborative map decisions into a navigable route preview with estimated timing. The first consumer is the multi-person itinerary collaboration drawer. A future one-click itinerary replication feature should reuse the same backend planner contract and frontend planning panel instead of rebuilding the planning rules.

## User Workflow

1. Users finish adding and voting on real map spots in the collaboration drawer.
2. The user clicks "one-click plan".
3. A planning panel opens with a default spot set:
   - Include spots whose consensus is `must` or `want`.
   - If no spots have that consensus, show all real collaboration spots as selectable candidates.
4. The panel lets the user:
   - Choose or clear a departure time.
   - Keep automatic ordering enabled, or manually adjust the spot order.
   - Include or exclude spots before planning.
   - Choose the existing route strategy, initially `SHORTEST_TIME`.
5. The user generates a preview.
6. The panel shows total distance, total travel time, estimated finish time when a departure time exists, and a segment timeline.

## Recommended Architecture

Use a backend itinerary planner preview endpoint as the reusable core:

`POST /api/itinerary-planner/preview`

This endpoint accepts a normalized list of real spots plus planning options. It returns the ordered route preview and timing metadata. The endpoint internally reuses the existing multi-spot navigation algorithm rather than duplicating shortest-path logic.

Frontend consumers use a reusable planner panel and a small composable:

- `ItineraryPlannerPanel.vue`: UI for selected spots, departure time, ordering mode, and preview result.
- `useItineraryPlanner.js`: request construction, validation, API call state, and presentation helpers.
- `ItineraryView.vue`: collaboration-specific adapter that converts real map nodes into planner input.

Future one-click itinerary replication can reuse the same panel and composable by passing replicated spots as planner input.

## Backend Contract

Request shape:

```json
{
  "departureTime": "2026-05-15T09:00:00",
  "strategy": "SHORTEST_TIME",
  "optimizeVisitOrder": true,
  "spots": [
    {
      "spotId": 130,
      "spotName": "China Aviation Museum",
      "latitude": 40.18030374722222,
      "longitude": 116.35033976111112,
      "transportMode": "walk"
    }
  ]
}
```

Response shape:

```json
{
  "orderedSpots": [],
  "segments": [],
  "totalDistance": 0,
  "totalTime": 0,
  "departureTime": "2026-05-15T09:00:00",
  "arrivalTime": "2026-05-15T10:30:00",
  "timeline": []
}
```

Timing rules:

- `totalTime` remains seconds, matching existing navigation responses.
- `arrivalTime` is only present when `departureTime` is supplied.
- Timeline entries are cumulative estimates from the planned segment order.
- If route data is incomplete, the endpoint still returns partial segments and marks warnings in the response.

## Spot-to-Route Resolution

The existing `/nav/route/multi-spot` endpoint requires `spotName` and optional node IDs. Collaborative map spots come from real destination candidates and usually do not include route node IDs.

For this feature, the planner service should resolve each spot to a navigable visit:

- Use `spotName` as the scenic area name.
- Use the scenic area's gate node as the default route node when no explicit node is supplied.
- Preserve the original destination identity in `orderedSpots` so UI actions can trace back to collaboration map spots.
- Skip spots that cannot be resolved to route data, and include a warning explaining why.

This keeps the collaboration workflow lightweight while preserving compatibility with the existing navigation data model.

## Frontend Integration

In the collaboration drawer:

- Add a compact "one-click plan" action near the collaboration map controls.
- Open `ItineraryPlannerPanel` as an embedded side panel or dialog within the drawer.
- Seed the panel from `tacticalNodes`.
- Use `must` and `want` consensus nodes by default.
- Keep conflict and backup nodes available but unchecked.
- Show disabled state when there are fewer than two selected spots.

Preview presentation:

- Summary: total distance, route time, departure time, estimated arrival time.
- Ordered stops: numbered list with included/excluded state.
- Segments: concise route stages using existing segment fields.
- Warnings: unresolved spots or missing city-route timing.

The panel should not depend on collaboration-specific objects. It accepts plain planner spots and emits a generated preview, which makes it suitable for itinerary replication.

## Reuse for One-Click Itinerary Replication

The future one-click itinerary replication flow can reuse this module as follows:

1. Replication extracts or recommends a list of destination spots.
2. It maps them into the same planner spot input shape.
3. It opens `ItineraryPlannerPanel` with those spots preselected.
4. It calls `POST /api/itinerary-planner/preview`.
5. It saves or displays the returned ordered route and timeline.

No collaboration vote data is required for replication. Vote consensus is only one adapter for choosing default selected spots.

## Error Handling

- No selected spots: show a local validation message.
- One selected spot: show a single-stop timeline but disable route generation that requires routing.
- Backend returns warnings: display them in the preview panel.
- Backend request fails: keep the selected spot state and show retry.
- Missing departure time: still compute duration and distance, but hide absolute arrival time.

## Testing

Backend:

- Planner service builds a preview from real spots and reuses multi-spot route output.
- Departure time produces expected arrival and cumulative timeline.
- Missing route data yields warnings instead of failing the whole preview.

Frontend:

- Planner input builder defaults to `must` and `want` consensus spots.
- Manual include/exclude and ordering create the expected request payload.
- Preview summary formats distance, duration, and times consistently.
- The collaboration drawer passes map nodes into the reusable panel without leaking vote-specific data into the panel internals.

## Out of Scope

- Persisting generated plans as saved itinerary versions.
- Full drag-and-drop itinerary editing across days.
- Hotel, dining, or rest-stop scheduling.
- Live traffic integration beyond existing AMap city-route rendering.
