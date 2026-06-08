package com.travel.system.service.nav;

import com.travel.system.dto.IndoorNavigationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndoorNavigationService {

    public IndoorNavigationResponse plan(String buildingName, String from, String to) {
        String normalizedBuilding = buildingName == null || buildingName.isBlank() ? "场馆A" : buildingName;
        List<String> path = List.of("大门", "一层大厅", "电梯1F", "电梯3F", "三层走廊", "302教室");
        List<String> steps = List.of(
                "从大门进入一层大厅",
                "步行到电梯1F",
                "乘电梯到3F",
                "沿三层走廊到302教室"
        );
        return new IndoorNavigationResponse(normalizedBuilding, path, steps, 86.0);
    }
}
