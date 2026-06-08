package com.travel.system.service.nav;

import com.travel.system.dto.IndoorNavigationResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndoorNavigationServiceTest {

    @Test
    void routesFromGateToRoomThroughElevator() {
        IndoorNavigationService service = new IndoorNavigationService();

        IndoorNavigationResponse response = service.plan("教学楼A", "大门", "302教室");

        assertThat(response.getPath()).containsExactly("大门", "一层大厅", "电梯1F", "电梯3F", "三层走廊", "302教室");
        assertThat(response.getSteps()).anyMatch(step -> step.contains("乘电梯"));
    }
}
