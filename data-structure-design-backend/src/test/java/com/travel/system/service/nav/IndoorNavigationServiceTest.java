package com.travel.system.service.nav;

import com.travel.system.dto.IndoorBuildingDemo;
import com.travel.system.dto.IndoorNavigationRequest;
import com.travel.system.dto.IndoorNavigationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndoorNavigationServiceTest {
    private final IndoorNavigationService service = new IndoorNavigationService();

    @Test
    void listsThreeDemoBuildingsForPresentation() {
        List<IndoorBuildingDemo> buildings = service.listBuildings();

        assertThat(buildings)
                .extracting(IndoorBuildingDemo::getId)
                .containsExactly("teaching-a", "heritage-museum", "visitor-center");
        assertThat(buildings)
                .allSatisfy(building -> {
                    assertThat(building.getPoints()).isNotEmpty();
                    assertThat(building.getConnections()).isNotEmpty();
                    assertThat(building.getDefaultFromNodeId()).isNotBlank();
                    assertThat(building.getDefaultToNodeId()).isNotBlank();
                    assertThat(building.getPoints()).allSatisfy(point -> {
                        assertThat(point.getX()).isBetween(0.0, 100.0);
                        assertThat(point.getY()).isBetween(0.0, 100.0);
                    });
                });
    }

    @Test
    void plansTeachingBuildingRouteThroughElevatorToRoom() {
        IndoorNavigationResponse response = service.plan(new IndoorNavigationRequest(
                "teaching-a",
                "t-gate",
                "t-room-302",
                "SHORTEST_TIME"
        ));

        assertThat(response.getBuildingName()).isEqualTo("智慧教学楼 A 座");
        assertThat(response.getSteps())
                .extracting(IndoorNavigationResponse.IndoorRouteStep::getAction)
                .contains("elevator");
        assertThat(response.getSteps())
                .extracting(IndoorNavigationResponse.IndoorRouteStep::getToName)
                .contains("302 多媒体教室");
        assertThat(response.getFloorSegments())
                .extracting(IndoorNavigationResponse.IndoorFloorSegment::getFloor)
                .contains(1, 3);
        assertThat(response.getTotalDistance()).isGreaterThan(0);
        assertThat(response.getSteps()).allSatisfy(step -> assertThat(step.getDistance()).isGreaterThan(0));
    }

    @Test
    void plansMuseumRouteAcrossMultipleFloors() {
        IndoorNavigationResponse response = service.plan(new IndoorNavigationRequest(
                "heritage-museum",
                "m-gate",
                "m-special-3",
                "SHORTEST_DISTANCE"
        ));

        assertThat(response.getSteps())
                .extracting(IndoorNavigationResponse.IndoorRouteStep::getInstruction)
                .anyMatch(text -> text.contains("1F 到 2F"))
                .anyMatch(text -> text.contains("2F 到 3F"));
        assertThat(response.getToName()).isEqualTo("三层临展厅");
        assertThat(response.getNotes()).anyMatch(text -> text.contains("跨越多个楼层"));
    }
}
