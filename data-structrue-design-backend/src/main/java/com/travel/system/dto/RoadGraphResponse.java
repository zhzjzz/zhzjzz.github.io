package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 道路图响应数据传输对象
 * <p>
 * 用于 REST API 返回道路网络图数据结构，包含节点和边两个集合，
 * 便于前端进行地图可视化和路径规划展示
 * </p>
 * <p>
 * 数据结构：
 * <ul>
 *     <li>nodes: 道路节点列表（路口、建筑物等位置点）</li>
 *     <li>edges: 道路边列表（节点之间的连接关系）</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see com.travel.system.model.RoadNode 道路节点实体
 * @see com.travel.system.model.RoadEdge 道路边实体
 */
@Data
@AllArgsConstructor
public class RoadGraphResponse {

    /**
     * 道路节点列表
     * 包含图中所有可用于导航的节点（路口、地标、建筑物等）
     */
    private List<RoadGraphNode> nodes;

    /**
     * 道路边列表
     * 包含图中所有节点之间的连接关系及道路属性
     */
    private List<RoadGraphEdge> edges;

    /**
     * 道路图节点数据传输对象
     * <p>
     * 简化的节点数据结构，用于前端展示和路径计算
     * </p>
     */
    @Data
    @AllArgsConstructor
    public static class RoadGraphNode {

        /**
         * 节点唯一标识符
         */
        private Long id;

        /**
         * 节点名称
         * 如"北门"、"主楼"等
         */
        private String name;

        /**
         * 节点类型
         * 如"入口"、"建筑"等
         */
        private String nodeType;

        /**
         * 纬度坐标
         * WGS84坐标系，用于地图定位
         */
        private Double latitude;

        /**
         * 经度坐标
         * WGS84坐标系，用于地图定位
         */
        private Double longitude;
    }

    /**
     * 道路图边数据传输对象
     * <p>
     * 简化的边数据结构，描述两个节点之间的道路连接
     * </p>
     */
    @Data
    @AllArgsConstructor
    public static class RoadGraphEdge {

        /**
         * 边唯一标识符
         */
        private Long id;

        /**
         * 起点节点 ID
         */
        private Long fromNodeId;

        /**
         * 终点节点 ID
         */
        private Long toNodeId;

        /**
         * 道路距离（米）
         * 两节点之间的实际道路长度
         */
        private Double distanceMeters;

        /**
         * 允许的交通工具类型
         * 逗号分隔，如"walk,bike,car"
         */
        private String allowedTransport;
    }
}
