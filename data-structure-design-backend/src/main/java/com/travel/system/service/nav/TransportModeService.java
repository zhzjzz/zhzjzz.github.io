package com.travel.system.service.nav;

import com.travel.system.model.nav.RoadEdge;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TransportModeService {

    public static final double SPEED_WALK = 1.2;
    public static final double SPEED_BIKE = 4.0;

    /**

     * 根据交通方式返回理想速度，作为时间估算的基础速度；电动车在后端统一使用 bike 表示。

     */
    public double getIdealSpeed(String transportMode) {
        return switch (transportMode != null ? transportMode.toLowerCase() : "walk") {
            case "bike" -> SPEED_BIKE;
            default -> SPEED_WALK;
        };
    }

    /**
     * 计算通过一条边的实际时间（秒）。
     * 时间 = length / (idealSpeed * congestionBase)
     */
    public double calculateTravelTime(RoadEdge edge, String transportMode) {
        double speed = getIdealSpeed(transportMode);
        double congestion = edge.getCongestionBase() != null ? edge.getCongestionBase() : 1.0;
        double length = edge.getLength() != null ? edge.getLength() : 0.0;
        if (speed * congestion <= 0) {
            return Double.MAX_VALUE;
        }
        return length / (speed * congestion);
    }

    /**
     * 判断一条边是否允许某种交通工具。
     */
    public boolean isVehicleAllowed(RoadEdge edge, String transportMode) {
        String mode = normalizeMode(transportMode);
        if (edge.getAllowedVehicles() == null || edge.getAllowedVehicles().isBlank()) {
            return "walk".equalsIgnoreCase(mode);
        }
        String[] vehicles = edge.getAllowedVehicles().toLowerCase().split(",");
        return Arrays.asList(vehicles).contains(mode);
    }

    /**
     * 过滤出允许混合交通模式的边。
     * 混合模式时，只要边允许模式中的任意一种即可通行。
     */
    public List<RoadEdge> filterEdgesByMixedMode(List<RoadEdge> edges, List<String> transportModes) {
        if (transportModes == null || transportModes.isEmpty()) {
            return edges;
        }
        Set<String> modeSet = new HashSet<>();
        transportModes.forEach(m -> modeSet.add(m.toLowerCase()));

        return edges.stream()
                .filter(e -> {
                    if (e.getAllowedVehicles() == null || e.getAllowedVehicles().isBlank()) {
                        return modeSet.contains("walk");
                    }
                    String[] vehicles = e.getAllowedVehicles().toLowerCase().split(",");
                    for (String v : vehicles) {
                        if (modeSet.contains(v.trim())) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    /**

     * 标准化交通方式字符串；空值默认步行，并将旧的 cart 参数兼容转换为数据库使用的 bike。

     */
    private String normalizeMode(String transportMode) {
        if (transportMode == null || transportMode.isBlank()) {
            return "walk";
        }
        String mode = transportMode.trim().toLowerCase();
        return "cart".equals(mode) ? "bike" : mode;
    }
}
