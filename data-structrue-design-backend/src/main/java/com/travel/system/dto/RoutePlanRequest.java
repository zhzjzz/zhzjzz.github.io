package com.travel.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 路线规划请求数据传输对象
 * <p>
 * 用于接收前端发送的路线规划请求参数，支持两种规划模式：
 * <ul>
 *     <li>单目标规划：从 fromNodeId 到 toNodeId 的最短路径</li>
 *     <li>多目标规划：从 fromNodeId 出发依次访问 targetNodeIds 中的点</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see RoutePlanResponse 路线规划响应对象
 */
@Data
public class RoutePlanRequest {

    /**
     * 起点节点 ID
     * 路线规划的出发点
     */
    private Long fromNodeId;

    /**
     * 终点节点 ID（单目标规划时使用）
     * 路线规划的到达点
     */
    private Long toNodeId;

    /**
     * 目标节点列表（多目标规划时使用）
     * 需要依次访问的多个目标点
     */
    private List<Long> targetNodeIds;

    /**
     * 规划策略
     * <ul>
     *     <li>"distance" - 距离最短（默认）</li>
     *     <li>"time" - 时间最短</li>
     * </ul>
     */
    private String strategy;

    /**
     * 交通方式
     * <ul>
     *     <li>"walk" - 步行（默认）</li>
     *     <li>"bike" - 骑行</li>
     *     <li>"drive" - 驾车</li>
     * </ul>
     */
    private String transport;
}
