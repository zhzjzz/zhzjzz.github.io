package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 路线规划响应数据传输对象
 * <p>
 * 返回路线规划的结果，包含：
 * <ul>
 *     <li>完整路径：经过的所有节点序列</li>
 *     <li>访问顺序：多目标规划时的目标点访问顺序</li>
 *     <li>总距离：路径的总长度（米）</li>
 *     <li>总时间：预计通行时间（分钟）</li>
 * </ul>
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see RoutePlanRequest 路线规划请求对象
 */
@Data
@AllArgsConstructor
public class RoutePlanResponse {

    /**
     * 完整路径节点 ID 列表
     * 按顺序包含从起点到终点经过的所有节点
     */
    private List<Long> pathNodeIds;

    /**
     * 访问顺序节点 ID 列表
     * 多目标规划时使用，记录依次访问的目标点顺序
     */
    private List<Long> visitOrderNodeIds;

    /**
     * 总距离（米）
     * 整个路径的累计长度
     */
    private Double totalDistanceMeters;

    /**
     * 总通行时间（分钟）
     * 根据道路速度和拥堵情况估算的时间
     */
    private Double totalTravelMinutes;
}
