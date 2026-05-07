# Frontend Backend URL Setup and Troubleshooting

This project is a Vite frontend deployed as static files, usually on GitHub Pages.
The backend can be exposed by ngrok during development.

## Quick Setup

1. Copy the environment template:

```powershell
Copy-Item .env.example .env
```

2. Edit `.env`:

```env
VITE_API_BASE_URL=https://your-ngrok-domain.ngrok-free.app/api
VITE_AMAP_KEY=your-amap-key
VITE_AMAP_SECRET=your-amap-security-js-code
```

3. Restart the frontend dev server after editing `.env`:

```powershell
npm run dev
```

For GitHub Pages, `.env` values are read at build time. After changing `.env`, rebuild and redeploy:

```powershell
npm run build
```

## Important Files

- Frontend HTTP client: `src/api/http.js`
- API wrapper functions: `src/api/travel.js`
- Environment template: `.env.example`
- Local environment file: `.env`
- Backend CORS config: `../data-structrue-design-backend/src/main/java/com/travel/system/config/CorsConfig.java`
- Backend SQLite config: `../data-structrue-design-backend/src/main/resources/application.yml`

## How Backend URL Is Resolved

`src/api/http.js` reads:

```js
import.meta.env.VITE_API_BASE_URL
```

If it is empty, it falls back to:

```js
/api
```

If the value is an ngrok host without `/api`, `http.js` appends `/api` automatically.

Valid examples:

```env
VITE_API_BASE_URL=https://abc.ngrok-free.app
VITE_API_BASE_URL=https://abc.ngrok-free.app/api
```

Both should work.

## Common Problems

### 1. Frontend still calls the old ngrok URL

Cause:
Vite reads `.env` only when the dev server starts.

Fix:

```powershell
Stop dev server
npm run dev
```

For GitHub Pages, rebuild and redeploy.

### 2. Browser shows CORS error

Cause:
Backend does not allow the frontend origin.

Check backend file:

```text
data-structrue-design-backend/src/main/java/com/travel/system/config/CorsConfig.java
```

Make sure it allows:

```text
http://localhost:5173
https://your-github-username.github.io
```

Also restart backend after changing CORS.

### 3. ngrok browser warning breaks API calls

The frontend already sends this header in `src/api/http.js`:

```js
ngrok-skip-browser-warning: 1
```

If API calls still return HTML instead of JSON, check the Network tab. The request may be going to the wrong URL.

### 4. Backend is not reachable

Check backend locally:

```powershell
curl http://localhost:8080/api/destinations/top?k=10
```

Then check ngrok:

```powershell
curl https://your-ngrok-domain.ngrok-free.app/api/destinations/top?k=10
```

If local works but ngrok fails, restart ngrok:

```powershell
ngrok http 8080
```

### 5. Port 8080 is already used

Find and stop the process:

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Or start backend on another port:

```powershell
$env:SERVER_PORT=8081
mvn spring-boot:run
```

If the backend port changes, ngrok must point to the same port.

### 6. SQLite data missing or navigation has no nodes

Backend SQLite path is configured in:

```text
data-structrue-design-backend/src/main/resources/application.yml
```

Current key:

```yaml
spring.datasource.hikari.jdbc-url
```

Expected database tables include:

```text
spots
nodes
edges
buildings
pois
city_routes
```

If navigation node dropdowns are empty, verify:

```sql
SELECT DISTINCT spot_name FROM nodes;
SELECT DISTINCT spot_name FROM edges;
SELECT * FROM buildings LIMIT 5;
SELECT * FROM pois LIMIT 5;
```

### 7. GitHub Pages works locally but not after deployment

Cause:
GitHub Pages serves static files. It cannot read your local `.env`.

Fix:
Set `VITE_API_BASE_URL` in the build environment used for deployment, then rebuild.

If deploying manually:

```powershell
npm run build
```

Then publish the generated `dist` folder.

## Commands for AI Debugging

Use these commands first:

```powershell
Get-Content src/api/http.js
Get-Content .env
npm run build
```

Backend:

```powershell
Get-Content src/main/resources/application.yml
mvn -q -DskipTests compile
mvn spring-boot:run
```

Check git changes:

```powershell
git status --short
```

## Do Not Commit

Do not commit local secrets or personal ngrok URLs:

```text
.env
.env.*
```

Commit this instead:

```text
.env.example
TROUBLESHOOTING.md
```
