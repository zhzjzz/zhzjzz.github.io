# 后端项目环境配置指南

## 快速开始 (本地开发)

团队成员只需以下步骤即可在本地运行后端：

### 1. 前置要求

- JDK 17+
- Maven 3.8+

### 2. 克隆并运行

```bash
git clone <仓库地址>
cd data-structrue-design-backend
mvn spring-boot:run
```

项目默认使用 `dev` profile，**无需任何额外配置**即可启动：
- 数据库：内嵌 H2（文件存储在 `data/dev-db/`）

### 3. 验证启动成功

- API 文档: http://localhost:8080/swagger-ui.html
- H2 控制台: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/dev-db/travel_system`
  - 用户名: `sa`，密码: 空

---

## 功能说明

| 功能 | dev (默认) | prod |
|------|-----------|------|
| 数据库 | H2 内嵌（零配置） | MySQL |
| 全文搜索 | MySQL LIKE 模糊查询 | MySQL LIKE 模糊查询 |
| 行程多人协作 | WebSocket 可用 | WebSocket 可用 |

所有 CRUD 功能（景点、美食、日记、行程等）以及 WebSocket 协作在 dev 和 prod 环境下均可正常使用。

---

## 切换到 MySQL (prod)

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

或者用 Docker 快速启动 MySQL：

```bash
docker compose -f docker-compose-dev.yml up -d
# MySQL 运行在 localhost:3307，用户: travel_user，密码: travel_pass
```

---

## 部署到阿里云学生机

### 步骤 1：购买并配置服务器

1. 访问 [阿里云学生机](https://www.aliyun.com/act/aliyun/campus) 完成学生认证
2. 购买 2核2G 云服务器（约 ￥9.9/月）
3. 选择操作系统：**Ubuntu 22.04**
4. 购买后进入控制台 → 实例 → 安全组 → 添加规则：
   - 端口 8080（应用）
   - 端口 22（SSH）

### 步骤 2：连接到服务器

```bash
ssh root@<你的服务器公网IP>
```

### 步骤 3：一键部署

在服务器上运行以下命令：

```bash
# 1. 安装 Git
apt update && apt install git -y

# 2. 克隆项目
git clone <仓库地址>
cd data-structrue-design-backend

# 3. 运行一键部署脚本（会自动安装 Docker、构建镜像、启动服务）
chmod +x deploy.sh
./deploy.sh
```

部署脚本会自动完成：
1. 安装 Docker 和 Docker Compose
2. 构建 Spring Boot 应用镜像（JVM 限制 256MB）
3. 启动 MySQL（限制 300MB）+ 应用（限制 400MB）
4. 等待应用就绪并输出访问地址

### 步骤 4：验证部署成功

```
API 文档: http://<服务器IP>:8080/swagger-ui.html
健康检查: http://<服务器IP>:8080/actuator/health
```

### 常见命令

```bash
# 查看日志
docker compose -f docker-compose-prod.yml logs -f app

# 重启服务
docker compose -f docker-compose-prod.yml restart

# 停止服务
docker compose -f docker-compose-prod.yml down

# 更新代码后重新部署
git pull
docker build -t travel-system:latest .
docker compose -f docker-compose-prod.yml up -d
```

### 内存预估

| 组件 | 限制内存 |
|------|---------|
| MySQL 8.0 | 300MB |
| Spring Boot JVM | 256MB |
| 操作系统 | ~500MB |
| **总计** | **~1GB** |

2核2G 服务器完全够用。

---

## 前端配合

前端需要配置后端 API 地址为 `http://<服务器IP>:8080`。

WebSocket 协作编辑连接方式：
```javascript
const socket = new SockJS('http://<服务器IP>:8080/ws');
const stomp = Stomp.over(socket);
stomp.connect({}, () => {
  // 订阅行程实时更新
  stomp.subscribe('/topic/itinerary/' + itineraryId, (msg) => {
    const update = JSON.parse(msg.body);
    if (update.type === 'UPDATED') {
      // 更新界面
    } else if (update.type === 'CONFLICT') {
      // 提示冲突，需要刷新
    }
  });
  // 发送编辑消息
  stomp.send('/app/itinerary/' + itineraryId + '/edit', {}, JSON.stringify({
    username: currentUser,
    expectedUpdatedAt: lastKnownTimestamp,
    name: newName,  // 只发改变的字段，其他字段为 null
  }));
});
```
