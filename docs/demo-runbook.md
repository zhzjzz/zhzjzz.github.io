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

## Tactical Map Collaboration Demo

1. Start the backend and frontend.
2. Log in with a demo user.
3. Open `协作行程`.
4. Choose an itinerary and click `协作`.
5. In the collaboration drawer, select a tactical map node and vote `必去`.
6. Open a second browser session with another demo user.
7. Vote `不想去` for the same node.
8. Confirm the node changes to a conflict state and the Ping panel shows both actions.
9. Disconnect the backend WebSocket or refresh during reconnect, then submit a vote and confirm the REST fallback saves it.
