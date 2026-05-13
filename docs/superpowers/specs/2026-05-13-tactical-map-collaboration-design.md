# 战术地图式多人协作设计

## 背景

当前旅游系统已经具备协作行程模块，前端行程页支持行程列表、详情、编辑和协作者字段，后端已有基于 STOMP WebSocket 的行程编辑广播与冲突消息能力。但现有呈现更接近表格管理，用户难以感知“多人正在一起规划”的过程。

本设计将多人协作升级为“战术地图投票层”：借鉴多人游戏里的地图 ping 机制，让同行成员在景点节点上快速表达偏好，实时同步团队选择，把协作从填写表单变成共同选点。

## 目标

- 在协作行程中增加一个可视化的战术地图区域，展示景点节点和团队投票状态。
- 支持成员对景点进行快速投票：必去、想去、不想去、备选。
- 实时广播投票结果，让其他在线成员立即看到节点状态变化。
- 通过队友动态和共识进度增强多人协作感。
- 保持第一版实现轻量，不引入复杂的回合制、实时位置或完整聊天系统。

## 非目标

- 不实现真实 GPS 位置共享。
- 不实现完整地图引擎替换或复杂 GIS 编辑能力。
- 不实现回合制 Ban/Pick 选点模式。
- 不实现权限复杂化，第一版沿用现有登录用户名作为投票身份。

## 页面结构

### 协作战术地图

展示当前行程的景点节点和路线关系。每个节点使用颜色表达团队共识：

- 红色：多数或全员选择 `必去`。
- 绿色：多数选择 `想去`。
- 黄色：出现明显分歧，例如多人选择 `不想去`。
- 灰色：暂无投票、备选或未形成共识。

第一版可以使用相对坐标节点图，不强依赖真实地图 SDK。后续可接入路线规划页已有的地图或路线数据。

### 队友 Ping 面板

展示最近协作动作，例如：

- 小周标记“外滩”为必去。
- 阿杰将“南京路”改为备选。
- 林同学反对“豫园”，理由是时间太紧。

该面板用于模拟多人游戏中的队友 ping feed，让用户感知协作正在发生。

### 景点决策卡

用户点击地图节点后打开决策卡，卡片包含：

- 景点名称和简要描述。
- 当前投票汇总。
- 队友投票理由。
- 投票按钮：`必去`、`想去`、`不想去`、`备选`。
- 当前用户上一次投票状态。

### 团队共识进度

展示当前行程的整体决策进度，例如：

```text
6 个景点中 4 个已达成共识
2 个景点存在分歧
1 个景点被标记为必去
```

该模块让多人规划从零散动作变成可推进的共同目标。

## 数据模型

新增模型 `ItinerarySpotVote`，用于保存用户对行程景点的投票。

```text
id
itinerary_id
spot_id
spot_name
username
vote_type
reason
created_at
updated_at
```

约束：

```text
unique(itinerary_id, spot_id, username)
```

同一用户对同一行程的同一景点只保留一条投票。用户重复投票时更新旧记录，避免重复票导致统计错误。

投票类型固定为：

```text
must
want
avoid
backup
```

含义：

- `must`：必去。
- `want`：想去。
- `avoid`：不想去。
- `backup`：备选。

## 后端设计

新增类建议：

- `ItinerarySpotVote`：投票模型。
- `ItinerarySpotVoteMapper`：投票数据访问。
- `ItinerarySpotVoteService`：投票校验、upsert、查询和汇总。
- `ItinerarySpotVoteController`：REST 查询和兜底投票接口。
- `ItineraryVoteWebSocketController`：实时投票入口。
- `ItinerarySpotVoteMessage`：客户端投票消息。
- `ItinerarySpotVoteBroadcastMessage`：服务端广播消息。

REST 接口：

```text
GET /itineraries/{id}/spot-votes
POST /itineraries/{id}/spot-votes
```

WebSocket：

```text
客户端发送：/app/itinerary/{id}/spot-vote
服务端广播：/topic/itinerary/{id}
```

广播消息增加类型：

```text
SPOT_VOTE_UPDATED
SPOT_VOTE_REJECTED
```

`SPOT_VOTE_UPDATED` 用于广播成功投票，`SPOT_VOTE_REJECTED` 用于广播非法类型、缺少用户、景点无效等错误。

## 前端设计

新增或拆分组件：

- `TacticalMapPanel`：展示节点、路线和状态色。
- `SpotDecisionCard`：展示节点详情、投票按钮和理由输入。
- `SquadPingFeed`：展示最近投票动态。
- `ConsensusProgress`：展示整体共识进度。

前端状态示例：

```js
{
  spotId: 101,
  spotName: '外滩',
  x: 42,
  y: 58,
  votes: [
    { username: '小周', type: 'must', reason: '夜景必看' },
    { username: '阿杰', type: 'want', reason: '顺路' }
  ],
  consensus: 'must'
}
```

第一版共识计算放在前端，降低后端复杂度：

- 全员或多数 `must`：红色。
- 多数 `want`：绿色。
- `avoid` 达到 2 人或占比明显：黄色。
- 无明显多数或 `backup` 为主：灰色。

## 实时流程

1. 用户进入行程页，加载行程和景点投票数据。
2. 前端订阅 `/topic/itinerary/{id}`。
3. 用户点击战术地图上的景点节点。
4. 前端打开 `SpotDecisionCard`。
5. 用户选择投票类型并可填写理由。
6. 前端通过 WebSocket 发送投票消息。
7. 后端校验消息并 upsert 投票记录。
8. 后端向 `/topic/itinerary/{id}` 广播投票结果。
9. 所有在线客户端更新节点颜色、队友 Ping 面板和共识进度。
10. 如果 WebSocket 不可用，前端使用 REST `POST /itineraries/{id}/spot-votes` 兜底提交。

## 异常处理

- 用户名缺失：前端阻止投票并提示“请先登录后参与协作”。
- 投票类型非法：后端拒绝并返回错误，WebSocket 场景广播 `SPOT_VOTE_REJECTED`。
- 景点信息缺失：前端禁用投票按钮，后端拒绝保存。
- WebSocket 断开：前端显示离线提示，并允许用户通过 REST 兜底提交。
- 多人同时投票：不需要冲突提示，因为每个用户只更新自己的投票。
- 重复投票：后端更新旧记录，前端将最新投票覆盖旧状态。

## 测试计划

- 同一用户重复投票时，数据库只保留一条记录。
- 多个用户投票后，节点状态色能按规则正确变化。
- WebSocket 广播后，另一个客户端能实时收到并更新节点。
- WebSocket 断开后，REST 兜底投票可用。
- 非法投票类型、缺少用户名、缺少景点信息能被拦截。
- 移动端点击节点、打开决策卡、提交投票流程可操作。
- 队友 Ping 面板按时间倒序显示最近动作。
- 共识进度能正确统计已达成共识、存在分歧和必去节点数量。

## 落地顺序

1. 使用 mock 数据完成 `TacticalMapPanel`、`SpotDecisionCard`、`SquadPingFeed` 和 `ConsensusProgress` 的静态交互。
2. 新增 `ItinerarySpotVote` 数据表、模型、Mapper 和 Service。
3. 新增 REST 查询和投票接口。
4. 新增 WebSocket 投票入口和广播消息类型。
5. 前端接入真实投票数据和实时广播。
6. 增加离线兜底、错误提示和移动端适配。
7. 做视觉打磨，使地图节点、队友动作和共识进度更接近多人游戏战术地图体验。

## 设计理由

相比只在行程表格里展示协作者，战术地图投票层能更直接地呈现“多人正在共同决策”。它与当前旅游系统的路线、景点和行程模块自然衔接，也能复用已有 WebSocket 协作基础。第一版将投票作为独立轻量模型，避免破坏现有行程 CRUD，同时为后续扩展路线协商、集合点、实时同行和 Ban/Pick 选点模式保留空间。
