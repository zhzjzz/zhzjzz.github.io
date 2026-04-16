## 个性化旅游系统（Vue3 + Java17 + Spring Boot + MySQL + Elasticsearch）

本仓库已生成一个可运行的前后端基础版本，覆盖以下核心能力：
- 目的地 Top-K 推荐与关键字检索
- 路线规划（最短距离/最短时间 + 交通工具通行约束）
- 旅游日记管理与全文检索（ES）
- 协作行程创建与查询
- 美食推荐 Top-K

### 目录结构

- `/data-structrue-design-frontend`：Vue3 前端（Vite）
- `/data-structrue-design-backend`：Spring Boot 后端（Java17）

### 后端启动

```bash
cd data-structrue-design-backend
mvn spring-boot:run
```

可通过环境变量配置：
- `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD`
- `ES_URIS`
- `SERVER_PORT`
- `GRAPHHOPPER_ENABLED`（默认 `false`）
- `GRAPHHOPPER_OSM_FILE`（默认 `data/osm/china-latest.osm.pbf`）
- `GRAPHHOPPER_GRAPH_LOCATION`（默认 `data/graph-cache`）
- `GRAPHHOPPER_PROFILE`（默认 `car`）

默认地址：`http://localhost:8080`

### GraphHopper + OSM 路由

1. 下载中国区 OSM 数据（`.osm.pbf`）并放置到 `data/osm/china-latest.osm.pbf`（可通过环境变量覆盖）。
2. 首次导入图数据耗时较长，请确保缓存目录有写权限。
3. 推荐用较大堆内存启动后端（全国数据）：

```bash
java -Xmx6g -Xms6g -jar your-project.jar
```

### 前端启动

```bash
cd data-structrue-design-frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

如需修改后端地址：
- 在前端环境变量中设置 `VITE_API_BASE_URL`（默认 `http://localhost:8080/api`）

### 已实现 API（示例）

- `GET /api/destinations`
- `GET /api/destinations/top?k=10`
- `POST /api/routes/plan`
- `GET /api/route?startLat=...&startLon=...&endLat=...&endLon=...`
- `GET /api/diaries`
- `POST /api/diaries`
- `GET /api/diaries/search?keyword=...`
- `GET /api/itineraries`
- `POST /api/itineraries`
- `GET /api/foods/top?k=10`
