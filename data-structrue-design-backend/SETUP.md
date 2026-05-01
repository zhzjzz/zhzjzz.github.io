# 后端项目环境配置指南

## 快速开始 (本地开发)

团队成员只需以下步骤即可在本地运行后端：

### 1. 前置要求

- JDK 17+
- Maven 3.8+
- (可选) Docker — 仅在需要测试 ES/MySQL 时

### 2. 克隆并运行

```bash
git clone <仓库地址>
cd data-structrue-design-backend
mvn spring-boot:run
```

项目默认使用 `dev` profile，**无需任何额外配置**即可启动：
- 数据库：内嵌 H2（文件存储在 `data/dev-db/`）
- Elasticsearch：关闭
- GraphHopper 路由：关闭

### 3. 验证启动成功

- API 文档: http://localhost:8080/swagger-ui.html
- H2 控制台: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/dev-db/travel_system`
  - 用户名: `sa`，密码: 空

---

## 功能说明

| 功能 | dev (默认) | prod |
|------|-----------|------|
| 数据库 | H2 内嵌 | MySQL |
| Elasticsearch | 关闭 | 开启 |
| GraphHopper 路由 | 关闭 | 开启 |
| 全文搜索 API | 降级为数据库查询 | 正常工作 |
| 路由规划 API | 返回 503 | 正常工作 |

所有核心 CRUD 功能（景点、美食、日记、行程等）在 dev 环境下均可正常使用。

---

## 切换到正式环境

### 方式一：环境变量

```bash
# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE="prod"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
mvn spring-boot:run

# Linux / macOS
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
mvn spring-boot:run
```

### 方式二：使用 .env 文件

```bash
cp .env.example .env
# 编辑 .env，填入实际的数据库密码、ES 地址等
```

---

## 使用 Docker 运行 MySQL + ES

如果本地没有 MySQL 和 Elasticsearch，可以用 Docker 一键启动：

```bash
docker compose -f docker-compose-dev.yml up -d
```

这会启动：
- MySQL 8.0 → `localhost:3307`（用户: travel_user，密码: travel_pass）
- Elasticsearch 8.12 → `localhost:9201`

然后以 prod profile 连接：

```bash
# Windows (PowerShell)
$env:SPRING_PROFILES_ACTIVE="prod"
$env:MYSQL_URL="jdbc:mysql://localhost:3307/travel_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
$env:MYSQL_USERNAME="travel_user"
$env:MYSQL_PASSWORD="travel_pass"
$env:ES_URIS="http://localhost:9201"
mvn spring-boot:run

# Linux / macOS
export SPRING_PROFILES_ACTIVE=prod
export MYSQL_URL="jdbc:mysql://localhost:3307/travel_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
export MYSQL_USERNAME=travel_user
export MYSQL_PASSWORD=travel_pass
export ES_URIS=http://localhost:9201
mvn spring-boot:run
```

---

## GraphHopper 路由功能

路由功能需要 OSM 地图数据文件。小型北京地图 (~36MB) 可用于开发测试。

如果需要完整中国地图路由：
1. 下载 OSM 数据文件到 `data/osm/` 目录
2. 设置环境变量 `GRAPHHOPPER_ENABLED=true`
3. 首次启动会构建图缓存，需要几分钟

---

## OSM 数据文件获取

北京地图（约 36MB，开发用）：
```
data/osm/beijing-260416.osm.pbf
```

完整中国地图（约 1.5GB，生产用）需单独提供，不提交到 Git。
