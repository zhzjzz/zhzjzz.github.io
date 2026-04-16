package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 道路边实体类
 * <p>
 * 表示道路网络中两个节点之间的连接边，是路径规划的基础数据结构。
 * 每条边包含起点、终点以及道路属性信息，用于 Dijkstra 等最短路径算法
 * </p>
 * <p>
 * 边的主要属性：
 * <ul>
 *     <li>距离：边的物理长度</li>
 *     <li>理想速度：该路段的建议通行速度</li>
 *     <li>拥堵系数：反映当前交通状况（0.0-1.0）</li>
 *     <li>允许通行的交通工具类型</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see RoadNode 道路节点实体
 */
@Entity
@Data
public class RoadEdge {

    /**
     * 边的唯一标识符
     * 自动生成的主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 起点节点
     * 多对一关联，延迟加载
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id")
    private RoadNode fromNode;

    /**
     * 终点节点
     * 多对一关联，延迟加载
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id")
    private RoadNode toNode;

    /**
     * 道路距离（米）
     * 两节点之间的实际道路长度
     */
    private Double distanceMeters;

    /**
     * 理想速度（米/分钟）
     * 该路段的建议通行速度
     */
    private Double idealSpeed;

    /**
     * 拥堵系数（0.0-1.0）
     * 值越大表示越畅通，越小越拥堵
     * 用于计算实际通行时间
     */
    private Double congestion;

    /**
     * 允许的交通工具类型
     * 逗号分隔的字符串，如 "walk,bike,car"
     * 表示该路段支持的通行方式
     */
    private String allowedTransport;
}
