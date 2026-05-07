# 个性化旅游系统

这是一个前后端分离的旅游推荐与路线规划系统。

当前项目已切换为本地 SQLite/GeoPackage 数据库，不再依赖 MySQL、H2、Docker 或 Linux 服务器部署。前端可以部署到 GitHub Pages，后端可以本地运行后通过 ngrok 暴露公网地址。

## 目录结构

```text
data-structure-design-frontend/   Vue 3 + Vite 前端
data-structure-design-backend/    Java 17 + Spring Boot 后端
docs/                             文档资料
```

注意：旧拼写目录 `data-structrue-*` 已废弃，不应再使用。

## 核心功能

- 目的地推荐 Top10
- 按评分、热度、综合得分推荐目的地
- 场所查询，数据来自 SQLite 中的 `buildings` 和 `pois`
- 景区内导航，支持最短距离和最优时间
- 多景区多地点路线规划
- 跨景区城市段使用高德实际道路导航绘制
- 同一景区内支持步行和电动车，电动车内部按数据库 `bike` 规则计算
- 旅游日记、行程、用户登录等基础功能

## 后端启动

### 环境要求

- JDK 17+
- Maven 3.8+
- 数据库文件：`data-structure-design-backend/data/tourism_system.gpkg`

### 启动命令

```powershell
cd D:\gitCode\zhzjzz\data-structure-design-backend
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

Swagger：

```text
http://localhost:8080/swagger-ui/index.html
```

健康检查：

```text
http://localhost:8080/actuator/health
```

### 后端配置

主要配置文件：

```text
data-structure-design-backend/src/main/resources/application.yml
```

默认数据库连接：

```yaml
spring:
  datasource:
    url: ${SQLITE_URL:jdbc:sqlite:data/tourism_system.gpkg}
```

如果从仓库根目录或其他目录启动，后端启动类会自动尝试查找：

```text
data/tourism_system.gpkg
data-structure-design-backend/data/tourism_system.gpkg
```

也可以手动指定：

```powershell
$env:SQLITE_URL="jdbc:sqlite:D:/gitCode/zhzjzz/data-structure-design-backend/data/tourism_system.gpkg"
mvn spring-boot:run
```

如果 8080 端口被占用：

```powershell
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

或关闭占用进程：

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## 前端启动

### 环境要求

- Node.js 18+
- npm

### 启动命令

```powershell
cd D:\gitCode\zhzjzz\data-structure-design-frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

### 前端环境变量

复制模板：

```powershell
Copy-Item .env.example .env
```

`.env` 示例：

```env
VITE_API_BASE_URL=https://your-ngrok-domain.ngrok-free.app/api
VITE_AMAP_KEY=your-amap-key
VITE_AMAP_SECRET=your-amap-security-js-code
```

说明：

- `VITE_API_BASE_URL` 是后端 API 地址。
- 本地开发可以用 `http://localhost:8080/api`。
- GitHub Pages 部署时建议填 ngrok 的静态域名，例如 `https://xxx.ngrok-free.app/api`。
- `VITE_AMAP_KEY` 和 `VITE_AMAP_SECRET` 用于高德地图。

修改 `.env` 后必须重启前端开发服务器。

## GitHub Pages 部署

前端是静态站点，可以部署到 GitHub Pages。

部署工作流文件：

```text
.github/workflows/deploy.yml
```

前端构建命令：

```powershell
cd data-structure-design-frontend
npm run build
```

构建产物：

```text
data-structure-design-frontend/dist
```

如果后端地址变化，需要重新构建前端，因为 Vite 的环境变量是在构建时写入的。

## ngrok 后端公网访问

本地启动后端后运行：

```powershell
ngrok http 8080
```

然后把 ngrok 地址写入前端 `.env`：

```env
VITE_API_BASE_URL=https://your-ngrok-domain.ngrok-free.app/api
```

不要把个人 `.env` 提交到 GitHub。提交 `.env.example` 即可。

## 数据库文件

默认数据库位置：

```text
data-structure-design-backend/data/tourism_system.gpkg
```

数据库中主要表：

- `spots`
- `nodes`
- `edges`
- `buildings`
- `pois`
- `city_routes`
- `destination`
- `diary`
- `facility`
- `food`
- `itinerary`
- `user_account`

如果导航没有节点或路线，优先检查：

```sql
SELECT DISTINCT spot_name FROM nodes;
SELECT DISTINCT spot_name FROM edges;
SELECT * FROM buildings LIMIT 5;
SELECT * FROM pois LIMIT 5;
```

## 导航说明

### 景区内导航

接口：

```text
POST /api/nav/route/plan
```

支持：

- 最短距离：`SHORTEST_DISTANCE`
- 最优时间：`SHORTEST_TIME`
- 步行：`walk`
- 电动车：前端显示“电动车”，后端内部使用数据库中的 `bike`

### 多景区多地点导航

接口：

```text
POST /api/nav/route/multi-spot
```

前端支持：

- 添加多个景区
- 每个景区选择多个地点
- 每个景区单独选择步行或电动车
- 景区内路线由后端 SQLite 路网规划
- 跨景区城市段由高德 Driving 绘制实际道路路线

路线顺序：

- 默认按用户选择顺序访问。
- 开启“优化景区内地点顺序”后，同一景区内会优化多个地点的访问顺序。
- 10 个点以内使用精确动态规划。
- 超过 10 个点使用近邻 + 2-opt 近似优化，避免耗时过长。

## 常见问题

### 1. 后端提示 SQLite 路径不存在

确认数据库文件存在：

```text
data-structure-design-backend/data/tourism_system.gpkg
```

或者显式设置：

```powershell
$env:SQLITE_URL="jdbc:sqlite:D:/gitCode/zhzjzz/data-structure-design-backend/data/tourism_system.gpkg"
```

### 2. 端口 8080 被占用

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### 3. 前端仍然请求旧 ngrok 地址

修改 `.env` 后重启开发服务器：

```powershell
npm run dev
```

GitHub Pages 上则需要重新构建并部署。

### 4. IDEA 反复生成旧目录 `data-structrue-design-backend`

这是 IntelliJ 旧构建缓存导致的。处理方式：

```text
File -> Invalidate Caches / Restart
```

并确保 Maven 导入的是：

```text
data-structure-design-backend/pom.xml
```

### 5. 电动车没有路线

数据库中电动车道路使用 `bike` 标记，例如：

```text
allowed_vehicles = walk,bike
```

前端仍显示“电动车”，后端内部会按 `bike` 进行路径规划。

## 不要提交的内容

- `.env`
- 本地 IDE 缓存
- 旧拼写目录 `data-structrue-*`
- 临时日志文件

根目录 `.gitignore` 和后端 `.gitignore` 已包含相关规则。
