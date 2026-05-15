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
3. Open the destination recommendation page and show the Top10 leaderboard.
4. Open route planning, click demo mode, then click route planning.
5. Show map path, total distance, total time, and segment list.
6. Open facility search to show nearby service search.
7. Open itinerary collaboration to show multi-person itinerary management.

## Backup Checks

- If the map does not load, verify `.env` has `VITE_AMAP_KEY` and `VITE_AMAP_SECRET`.
- If API calls fail, verify backend is running at `http://localhost:8080`.
- If route demo finds no data, search and select a scenic spot manually.

## Tactical Map Collaboration Demo

1. Start the backend and frontend from `codex/tactical-map-collaboration`.
2. Confirm `data-structure-design-frontend/.env` has `VITE_AMAP_KEY` and `VITE_AMAP_SECRET`.
3. Open `http://localhost:5173/#/itineraries`.
4. Choose an itinerary and click collaboration.
5. Search a real destination such as `西湖`.
6. Add a result with latitude and longitude to the collaboration map.
7. Confirm the AMap panel shows a marker at the real location.
8. Vote "must go" for that marker.
9. Open a second browser session with another demo user and vote "avoid" for the same marker.
10. Confirm the marker changes to conflict state and the Ping panel shows both actions.
11. Stop WebSocket connectivity or refresh during reconnect, submit another vote, and confirm REST fallback saves it.
12. Click `一键规划`.
13. Confirm the planner panel preselects `must` and `want` spots.
14. Choose a departure time and keep automatic ordering enabled.
15. Click `生成`.
16. Confirm the preview shows total distance, total travel time, estimated arrival time, and segment timeline.
17. Note that future one-click itinerary replication can reuse the same `POST /api/itinerary-planner/preview` endpoint and `ItineraryPlannerPanel` by passing replicated destination spots into the same input shape.
