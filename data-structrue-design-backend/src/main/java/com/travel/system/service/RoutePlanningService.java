package com.travel.system.service;

import com.travel.system.dto.RoutePlanResponse;
import com.travel.system.model.RoadEdge;
import com.travel.system.mapper.RoadEdgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * 路线规划服务类
 * <p>
 * 提供基于图论算法的路径规划功能，支持以下特性：
 * <ul>
 *     <li>单目标最短路径规划（支持按距离或时间最优）</li>
 *     <li>多目标路径规划（类似旅行商问题）</li>
 *     <li>支持多种交通工具类型过滤</li>
 *     <li>基于 Dijkstra 算法实现最短路径计算</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 */
@Service
public class RoutePlanningService {

    /**
     * 道路边数据访问映射器
     * 用于查询道路网络的边信息（节点间连接关系）
     */
    private final RoadEdgeMapper roadEdgeMapper;

    /**
     * 构造函数，注入道路边数据访问依赖
     *
     * @param roadEdgeMapper 道路边数据访问映射器
     */
    public RoutePlanningService(RoadEdgeMapper roadEdgeMapper) {
        this.roadEdgeMapper = roadEdgeMapper;
    }

    /**
     * 计算两点之间的最短路径
     * <p>
     * 使用 Dijkstra 算法计算从起点到终点的最优路径，
     * 可根据策略选择按距离最短或时间最短进行规划
     * </p>
     *
     * @param fromId    起点节点ID
     * @param toId      终点节点ID
     * @param strategy  规划策略，可选值："distance"(距离最短) 或 "time"(时间最短)
     * @param transport 交通工具类型，如 "walk"(步行)、"car"(驾车) 等
     * @return 路线规划响应对象，包含完整路径、总距离和总耗时
     * @throws ResponseStatusException 当参数无效或路径不存在时抛出
     */
    public RoutePlanResponse shortestPath(Long fromId, Long toId, String strategy, String transport) {
        PathResult result = computePath(fromId, toId, strategy, transport);
        return new RoutePlanResponse(result.pathNodeIds(), List.of(toId),
                result.totalDistanceMeters(), result.totalTravelMinutes());
    }

    /**
     * 多目标路径规划（旅行商问题简化版）
     * <p>
     * 从起点出发，依次访问多个目标节点，最后返回起点。
     * 使用贪心算法选择下一个最近的未访问目标：
     * <ol>
     *     <li>从当前位置计算到所有未访问目标的最短路径</li>
     *     <li>选择成本最低的目标作为下一个访问点</li>
     *     <li>重复直到所有目标都被访问</li>
     *     <li>最后从最后一个目标返回起点</li>
     * </ol>
     * </p>
     *
     * @param fromId         起点节点ID
     * @param targetNodeIds  目标节点ID列表，需要依次访问的多个景点或地点
     * @param strategy       规划策略，"distance" 或 "time"
     * @param transport      交通工具类型
     * @return 包含完整访问顺序、路径、总距离和总耗时的响应对象
     * @throws ResponseStatusException 当目标列表为空、存在无法到达的目标、或无法返回起点时抛出
     */
    public RoutePlanResponse multiTargetPath(Long fromId, List<Long> targetNodeIds,
                                             String strategy, String transport) {
        // 验证目标节点列表
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "多目标规划至少需要一个目标节点");
        }

        // 过滤掉空值和与起点相同的目标，使用 LinkedHashSet 保持顺序并去重
        LinkedHashSet<Long> remainingTargets = new LinkedHashSet<>();
        for (Long targetId : targetNodeIds) {
            if (targetId == null) {
                continue;
            }
            if (!targetId.equals(fromId)) {
                remainingTargets.add(targetId);
            }
        }

        // 验证是否还有有效的目标节点
        if (remainingTargets.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "目标节点不能全部与起点相同");
        }

        // 初始化路径追踪变量
        List<Long> visitOrder = new ArrayList<>();      // 记录实际访问顺序
        List<Long> fullPath = new ArrayList<>();        // 完整路径节点序列
        fullPath.add(fromId);

        long current = fromId;
        double totalDistance = 0.0;
        double totalTime = 0.0;

        // 贪心算法：每次选择最近的未访问目标
        while (!remainingTargets.isEmpty()) {
            long currentNode = current;
            // 计算到所有剩余目标的路径，选择成本最低的
            PathChoice nextChoice = remainingTargets.stream()
                    .map(targetId -> new PathChoice(targetId,
                            computePath(currentNode, targetId, strategy, transport)))
                    .filter(choice -> !choice.pathResult().pathNodeIds().isEmpty())
                    .min(Comparator.comparingDouble(choice -> choice.pathResult().cost(strategy)))
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "存在无法到达的目标节点，请检查道路图连通性"));

            // 追加路径段到完整路径
            appendSegment(fullPath, nextChoice.pathResult().pathNodeIds());
            totalDistance += nextChoice.pathResult().totalDistanceMeters();
            totalTime += nextChoice.pathResult().totalTravelMinutes();
            current = nextChoice.targetNodeId();
            visitOrder.add(nextChoice.targetNodeId());
            remainingTargets.remove(nextChoice.targetNodeId());
        }

        // 从最后一个目标返回起点
        PathResult returnPath = computePath(current, fromId, strategy, transport);
        if (returnPath.pathNodeIds().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "无法从最后一个目标点返回起点，请检查道路图连通性");
        }
        appendSegment(fullPath, returnPath.pathNodeIds());
        totalDistance += returnPath.totalDistanceMeters();
        totalTime += returnPath.totalTravelMinutes();

        return new RoutePlanResponse(fullPath, visitOrder, totalDistance, totalTime);
    }

    /**
     * 计算从起点到所有可达节点的最短距离映射
     * <p>
     * 使用 Dijkstra 算法计算单源最短路径，
     * 返回从起点到每个节点的最短距离
     * </p>
     *
     * @param fromId    起点节点ID
     * @param transport 交通工具类型
     * @return 节点ID到最短距离的映射表，key为节点ID，value为距离（米）
     */
    public Map<Long, Double> shortestDistanceMap(Long fromId, String transport) {
        Map<Long, Double> dist = new HashMap<>();
        runShortestPath(fromId, "distance", transport, dist, new HashMap<>());
        return dist;
    }

    /**
     * 计算两点之间的路径（Dijkstra算法实现）
     * <p>
     * 内部核心方法，执行完整的 Dijkstra 最短路径计算：
     * <ol>
     *     <li>运行 Dijkstra 算法获取最短路径树</li>
     *     <li>根据前驱节点表重建实际路径</li>
     *     <li>遍历路径计算总距离和总时间</li>
     * </ol>
     * </p>
     *
     * @param fromId    起点节点ID
     * @param toId      终点节点ID
     * @param strategy  规划策略，影响边的权重计算
     * @param transport 交通工具类型，用于边过滤
     * @return 路径结果对象，包含节点序列、总距离、总耗时；若不可达则返回空路径和无穷大成本
     */
    private PathResult computePath(Long fromId, Long toId, String strategy, String transport) {
        // 存储最短距离和前驱节点
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        runShortestPath(fromId, strategy, transport, dist, prev);

        // 根据前驱节点表重建路径
        List<Long> path = buildPath(prev, fromId, toId);
        if (path.isEmpty()) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        // 遍历路径计算总距离和总时间
        double totalDistance = 0.0;
        double totalTimeMinutes = 0.0;
        for (int i = 0; i + 1 < path.size(); i++) {
            Long s = path.get(i);
            Long t = path.get(i + 1);
            // 查找对应的道路边
            RoadEdge edge = roadEdgeMapper.findByFromNodeId(s).stream()
                    .filter(e -> e.getToNode().getId().equals(t))
                    .findFirst()
                    .orElse(null);
            if (edge != null) {
                totalDistance += safe(edge.getDistanceMeters());
                totalTimeMinutes += travelTime(edge);
            }
        }
        return new PathResult(path, totalDistance, totalTimeMinutes);
    }

    /**
     * Dijkstra 最短路径算法核心实现
     * <p>
     * 标准 Dijkstra 算法，使用优先队列优化：
     * <ol>
     *     <li>初始化：起点距离为0，其他为无穷大</li>
     *     <li>取出当前距离最小的节点</li>
     *     <li>松弛操作：更新邻接节点的最短距离</li>
     *     <li>重复直到队列为空</li>
     * </ol>
     * 时间复杂度：O((V+E)logV)，其中 V 为顶点数，E 为边数
     * </p>
     *
     * @param fromId    起点节点ID
     * @param strategy  规划策略，决定边的权重（距离或时间）
     * @param transport 交通工具类型，用于过滤不可通行的边
     * @param dist      距离映射表，存储到各节点的最短距离（输出参数）
     * @param prev      前驱节点映射表，用于路径重建（输出参数）
     */
    private void runShortestPath(Long fromId,
                                 String strategy,
                                 String transport,
                                 Map<Long, Double> dist,
                                 Map<Long, Long> prev) {
        // 优先队列，按当前已知最短距离排序
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));
        dist.put(fromId, 0.0);
        pq.offer(new NodeDistance(fromId, 0.0));

        // Dijkstra 主循环
        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            // 跳过已过时的队列项
            if (current.distance() > dist.getOrDefault(current.nodeId(), Double.MAX_VALUE)) {
                continue;
            }

            // 遍历当前节点的所有出边
            for (RoadEdge edge : roadEdgeMapper.findByFromNodeId(current.nodeId())) {
                // 检查交通工具是否允许通行
                if (!allow(edge.getAllowedTransport(), transport)) {
                    continue;
                }
                long next = edge.getToNode().getId();
                // 根据策略计算边的权重
                double weight = "time".equalsIgnoreCase(strategy) ? travelTime(edge) : safe(edge.getDistanceMeters());
                double candidate = current.distance() + weight;
                // 松弛操作：发现更短路径
                if (candidate < dist.getOrDefault(next, Double.MAX_VALUE)) {
                    dist.put(next, candidate);
                    prev.put(next, current.nodeId());
                    pq.offer(new NodeDistance(next, candidate));
                }
            }
        }
    }

    /**
     * 根据前驱节点表重建路径
     * <p>
     * 从终点回溯到起点，使用前驱节点表构建完整路径
     * </p>
     *
     * @param prev  前驱节点映射表，key为节点，value为其前驱
     * @param fromId 起点节点ID
     * @param toId   终点节点ID
     * @return 从起点到终点的节点ID列表；若不可达则返回空列表
     */
    private List<Long> buildPath(Map<Long, Long> prev, Long fromId, Long toId) {
        LinkedList<Long> path = new LinkedList<>();
        Long cur = toId;
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(fromId)) {
                return path;
            }
            cur = prev.get(cur);
        }
        return List.of();
    }

    /**
     * 将路径段追加到完整路径中
     * <p>
     * 避免重复添加连接点（路径段的第一个节点与完整路径的最后一个节点相同）
     * </p>
     *
     * @param fullPath 完整路径列表（会被修改）
     * @param segment  要追加的路径段
     */
    private void appendSegment(List<Long> fullPath, List<Long> segment) {
        if (segment.isEmpty()) {
            return;
        }
        // 跳过第一个节点（与当前路径末尾重复）
        int startIndex = fullPath.isEmpty() ? 0 : 1;
        for (int i = startIndex; i < segment.size(); i++) {
            fullPath.add(segment.get(i));
        }
    }

    /**
     * 检查指定的交通工具是否允许通行
     * <p>
     * 边的 allowedTransport 字段为多值字符串，逗号分隔，
     * 如 "walk,car" 表示步行和驾车均可通行
     * </p>
     *
     * @param allowedTransport 边允许通行的交通工具列表（逗号分隔），可为null表示允许所有
     * @param currentTransport 当前使用的交通工具
     * @return true 如果当前交通工具被允许通行，否则 false
     */
    private boolean allow(String allowedTransport, String currentTransport) {
        if (allowedTransport == null || currentTransport == null) {
            return true;
        }
        return Arrays.stream(allowedTransport.split(","))
                .map(String::trim)
                .anyMatch(v -> v.equalsIgnoreCase(currentTransport));
    }

    /**
     * 计算通过某条边的预计通行时间
     * <p>
     * 时间 = 距离 / (理想速度 × 拥堵系数)
     * 拥堵系数范围通常为 0.1 ~ 1.0，值越小表示越拥堵
     * </p>
     *
     * @param edge 道路边信息
     * @return 预计通行时间（分钟）；若无法通行返回无穷大
     */
    private double travelTime(RoadEdge edge) {
        double speed = safe(edge.getIdealSpeed()) * Math.max(0.1, safe(edge.getCongestion()));
        return speed <= 0 ? Double.MAX_VALUE : safe(edge.getDistanceMeters()) / speed;
    }

    /**
     * 安全获取 Double 值，避免空指针
     *
     * @param v 可能为null的Double值
     * @return 值本身，或为null时返回 0.0
     */
    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    /**
     * Dijkstra 算法优先队列中的节点距离记录
     * <p>
     * Java 14+ 的 record 类型，用于存储节点ID和当前已知最短距离
     * </p>
     *
     * @param nodeId   节点ID
     * @param distance 当前已知的最短距离
     */
    private record NodeDistance(Long nodeId, double distance) {
    }

    /**
     * 路径计算结果内部记录类
     * <p>
     * 封装单条路径的计算结果，包含节点序列、总距离、总时间
     * </p>
     *
     * @param pathNodeIds         路径上的节点ID序列
     * @param totalDistanceMeters 总距离（米）
     * @param totalTravelMinutes  总耗时（分钟）
     */
    private record PathResult(List<Long> pathNodeIds, double totalDistanceMeters, double totalTravelMinutes) {
        /**
         * 根据策略获取路径成本
         *
         * @param strategy "time" 返回时间成本，其他返回距离成本
         * @return 对应的成本值
         */
        private double cost(String strategy) {
            return "time".equalsIgnoreCase(strategy) ? totalTravelMinutes : totalDistanceMeters;
        }
    }

    /**
     * 多目标规划中的路径选择记录
     * <p>
     * 用于存储从当前位置到某个目标节点的路径选择
     * </p>
     *
     * @param targetNodeId 目标节点ID
     * @param pathResult   到该目标的路径结果
     */
    private record PathChoice(Long targetNodeId, PathResult pathResult) {
    }
}
