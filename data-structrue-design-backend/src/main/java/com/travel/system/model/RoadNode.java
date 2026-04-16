package com.travel.system.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 道路节点实体类
 * <p>
 * 表示道路网络中的节点（位置点），是路径规划的基础数据结构。
 * 节点代表地图上的具体位置，如：
 * <ul>
 *     <li>建筑物入口</li>
 *     <li>路口交汇处</li>
 *     <li>重要地标</li>
 *     <li>校园主要地点</li>
 * </ul>
 * </p>
 * <p>
 * 节点之间通过 {@link RoadEdge} 连接形成道路网络图，
 * 用于 Dijkstra 等最短路径算法的计算
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see RoadEdge 道路边实体
 */
@Entity
@Data
public class RoadNode {

    /**
     * 节点唯一标识符
     * 自动生成的主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 节点名称
     * 如"北门"、"主楼"、"图书馆"等
     * 必填字段
     */
    @Column(nullable = false)
    private String name;

    /**
     * 节点类型
     * 如"入口"、"建筑"、"路口"等
     */
    private String nodeType;

    /**
     * 纬度坐标（WGS84坐标系）
     * 用于地图定位和距离计算
     */
    private Double latitude;

    /**
     * 经度坐标（WGS84坐标系）
     * 用于地图定位和距离计算
     */
    private Double longitude;
}
