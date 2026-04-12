package com.travel.system.controller;

import com.travel.system.dto.RoadGraphResponse;
import com.travel.system.model.RoadEdge;
import com.travel.system.model.RoadNode;
import com.travel.system.mapper.RoadEdgeMapper;
import com.travel.system.mapper.RoadNodeMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code RoadNodeController} 负责处理道路网络节点（RoadNode）相关的 HTTP 请求。
 *
 * <p>道路网络是路径规划与空间查询的核心数据结构，每个节点代表地图上的一个位置（路口、建筑物入口等），
 * 节点之间通过 {@link RoadEdge} 连接形成可通行的路径图。
 *
 * <p>提供两类接口：
 *
 * <ul>
 *   <li>查询全部道路节点；</li>
 *   <li>获取完整的道路图结构（节点+边），供前端可视化或路径规划算法使用。</li>
 * </ul>
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/road-nodes")
public class RoadNodeController {

    /** 道路节点持久层仓库。 */
private final RoadNodeMapper roadNodeRepository;

    /** 道路边持久层仓库，用于查询节点之间的连接关系。 */
private final RoadEdgeMapper roadEdgeRepository;

    /**
     * 构造函数注入依赖。
     *
     * @param roadNodeRepository 道路节点数据访问层
     * @param roadEdgeRepository 道路边数据访问层
     */
public RoadNodeController(RoadNodeMapper roadNodeRepository,
                              RoadEdgeMapper roadEdgeRepository) {
        this.roadNodeRepository = roadNodeRepository;
        this.roadEdgeRepository = roadEdgeRepository;
    }

    /**
     * 查询全部道路节点。
     *
     * @return 所有 {@link RoadNode} 实体列表
     */
    @GetMapping
    public List<RoadNode> list() {
        return roadNodeRepository.findAll();
    }

    /**
     * 获取完整的道路图结构（节点与边）。
     *
     * <p>返回的 {@link RoadGraphResponse} 包含：
     *
     * <ul>
     *   <li>节点列表：每个节点的 ID、名称、类型、经纬度坐标；</li>
     *   <li>边列表：每条边的 ID、起点、终点、距离（米）、允许的通行方式。</li>
     * </ul>
     *
     * <p>前端可基于此数据在地图上绘制道路网络，或用于本地路径规划算法的输入。
     *
     * @return 包含节点与边的道路图响应对象
     */
    @GetMapping("/graph")
    public RoadGraphResponse graph() {
        // 将数据库中的 RoadNode 实体转换为前端所需的简化节点结构
        List<RoadGraphResponse.RoadGraphNode> nodes = roadNodeRepository.findAll().stream()
                .map(node -> new RoadGraphResponse.RoadGraphNode(
                        node.getId(),
                        node.getName(),
                        node.getNodeType(),
                        node.getLatitude(),
                        node.getLongitude()
                ))
                .toList();

        // 将数据库中的 RoadEdge 实体转换为前端所需的简化边结构
        List<RoadGraphResponse.RoadGraphEdge> edges = roadEdgeRepository.findAll().stream()
                .map(this::toEdge)
                .toList();

        return new RoadGraphResponse(nodes, edges);
    }

    /**
     * 将 {@link RoadEdge} 实体转换为 {@link RoadGraphResponse.RoadGraphEdge} DTO。
     *
     * @param edge JPA 实体
     * @return 用于前端渲染的边数据结构
     */
    private RoadGraphResponse.RoadGraphEdge toEdge(RoadEdge edge) {
        return new RoadGraphResponse.RoadGraphEdge(
                edge.getId(),
                edge.getFromNode().getId(),
                edge.getToNode().getId(),
                edge.getDistanceMeters(),
                edge.getAllowedTransport()
        );
    }
}
